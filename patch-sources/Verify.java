import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.lang.reflect.*;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

public class Verify {
  static URLClassLoader loader(Path jar, String[] deps) throws Exception {
    List<URL> urls = new ArrayList<>(); urls.add(jar.toUri().toURL());
    for (String dep : deps) urls.add(Path.of(dep).toUri().toURL());
    return new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
  }
  public static void main(String[] args) throws Exception {
    Path oldJar = Path.of(args[0]), newJar = Path.of(args[1]);
    String[] deps = Arrays.copyOfRange(args, 2, args.length);
    try (URLClassLoader cl = loader(oldJar, deps)) {
      try {
        Class.forName("dev.replayconverter.ReplayConverter", false, cl).getDeclaredMethods();
        throw new AssertionError("Original class unexpectedly passed JVM verification");
      } catch (VerifyError expected) {
        if (!expected.getMessage().contains("Expecting a stack map frame") || !expected.getMessage().contains(Rebuild.METHOD)) throw expected;
        System.out.println("PASS: original JAR reproduces the exact JVM VerifyError in " + Rebuild.METHOD);
      }
    }
    try (URLClassLoader cl = loader(newJar, deps); JarFile jar = new JarFile(newJar.toFile())) {
      Class<?> converter = Class.forName("dev.replayconverter.ReplayConverter", true, cl);
      converter.getDeclaredMethods();
      Class<?> packet = cl.loadClass("dev.replayconverter.McprPacket");
      Class<?> translator = cl.loadClass("dev.replayconverter.viaversion.ProtocolTranslator");
      Method method = converter.getDeclaredMethod(Rebuild.METHOD, translator, packet, int.class, java.util.function.Consumer.class);
      method.setAccessible(true);
      Constructor<?> ctor = packet.getConstructor(int.class, byte[].class);
      for (byte[] data : new byte[][] {new byte[0], {0}, {0x24, 1, 2}, {(byte)0xFF, 0x01, (byte)0x80}}) {
        Object p = ctor.newInstance(123, data);
        byte[] actual = (byte[])method.invoke(null, null, p, 1, null);
        if (!Arrays.equals(actual, data)) throw new AssertionError("Payload changed");
      }
      System.out.println("PASS: replacement class loads, initializes and invokes under -Xverify:all; four payload cases are unchanged with null translator/consumer.");
      StringWriter diagnostics = new StringWriter();
      CheckClassAdapter.verify(new ClassReader(jar.getInputStream(jar.getJarEntry(Rebuild.TARGET))), cl, false, new PrintWriter(diagnostics));
      if (!diagnostics.toString().isBlank()) throw new AssertionError(diagnostics.toString());
      System.out.println("PASS: ASM CheckClassAdapter verification of full ReplayConverter class produces no diagnostics.");
    }
    try (JarFile before = new JarFile(oldJar.toFile()); JarFile after = new JarFile(newJar.toFile())) {
      if (before.size() != after.size()) throw new AssertionError("Entry count differs");
      int preserved = 0;
      for (JarEntry entry : Collections.list(before.entries())) {
        JarEntry replacement = after.getJarEntry(entry.getName());
        if (replacement == null) throw new AssertionError("Missing " + entry.getName());
        byte[] a = before.getInputStream(entry).readAllBytes(), b = after.getInputStream(replacement).readAllBytes();
        if (entry.getName().equals(Rebuild.TARGET)) continue;
        if (entry.getName().equals("fabric.mod.json")) {
          String x = new String(a, java.nio.charset.StandardCharsets.UTF_8).replace("\"version\":\"0.1.2-dev\"", "\"version\":\"0.1.3-dev\"");
          if (!x.equals(new String(b, java.nio.charset.StandardCharsets.UTF_8))) throw new AssertionError("Unexpected metadata change");
          continue;
        }
        if (!Arrays.equals(a, b)) throw new AssertionError("Unexpected change: " + entry.getName());
        preserved++;
      }
      System.out.println("PASS: " + preserved + " other JAR entries are byte-for-byte unchanged; fabric.mod.json changes only version. Protocol 767 checks, Flashback compatibility/mixins and bundled libraries preserved.");
    }
    System.out.println("Runtime: " + System.getProperty("java.version"));
    System.out.println("Scope: verifier and payload regression checks; no in-game replay conversion performed.");
  }
}
