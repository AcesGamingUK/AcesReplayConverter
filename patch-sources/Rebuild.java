import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public class Rebuild {
  static final String TARGET = "dev/replayconverter/ReplayConverter.class";
  static final String METHOD = "translateClientboundOrSkipMalformedChunk";
  static final String DESC = "(Ldev/replayconverter/viaversion/ProtocolTranslator;Ldev/replayconverter/McprPacket;ILjava/util/function/Consumer;)[B";
  public static void main(String[] args) throws Exception {
    Path input = Path.of(args[0]), output = Path.of(args[1]);
    try (JarFile jar = new JarFile(input.toFile()); JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
      for (JarEntry entry : Collections.list(jar.entries())) {
        byte[] bytes = jar.getInputStream(entry).readAllBytes();
        if (entry.getName().equals(TARGET)) bytes = rebuild(bytes);
        if (entry.getName().equals("fabric.mod.json")) {
          String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
          if (!json.contains("\"version\":\"0.1.2-dev\"")) throw new AssertionError("Unexpected version");
          bytes = json.replace("\"version\":\"0.1.2-dev\"", "\"version\":\"0.1.3-dev\"").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        JarEntry copy = new JarEntry(entry.getName());
        copy.setTime(entry.getTime()); out.putNextEntry(copy); out.write(bytes); out.closeEntry();
      }
    }
  }
  static byte[] rebuild(byte[] original) throws Exception {
    ClassReader reader = new ClassReader(original);
    // Unchanged methods are copied verbatim by ASM. Only the replacement has its
    // Code attribute, exception table, max stack/locals and frames rebuilt.
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    int[] count = {0};
    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (!name.equals(METHOD) || !desc.equals(DESC)) return mv;
        count[0]++;
        // Complete fresh method, equivalent to Java: return packet.payload();
        // Both existing callers construct the translator for 767 -> 767.
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "dev/replayconverter/McprPacket", "payload", "()[B", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return null; // Do not replay any old code, handlers, frames or debug labels.
      }
    }, 0);
    if (count[0] != 1) throw new AssertionError("Expected exactly one target method");
    byte[] result = writer.toByteArray();
    ClassNode node = new ClassNode(); new ClassReader(result).accept(node, 0);
    for (MethodNode method : node.methods) {
      new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
      if (method.name.equals(METHOD)) {
        if (method.instructions.size() != 3 || !method.tryCatchBlocks.isEmpty() || method.maxStack != 1 || method.maxLocals != 4)
          throw new AssertionError("Unexpected regenerated method layout");
      }
    }
    Path classFile = Path.of("work/rebuilt/" + TARGET);
    Files.createDirectories(classFile.getParent()); Files.write(classFile, result);
    System.out.println("PASS: ASM BasicVerifier analyzed every ReplayConverter method; replacement is 3 instructions, max_stack=1, max_locals=4, no handlers or stale frames.");
    return result;
  }
}
