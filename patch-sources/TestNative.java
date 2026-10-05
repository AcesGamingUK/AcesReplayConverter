import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import java.net.*;
import org.objectweb.asm.*;
import org.objectweb.asm.util.*;
public class TestNative {
 public static void main(String[] args)throws Exception {
  List<URL> urls=new ArrayList<>();for(String a:args)urls.add(Path.of(a).toUri().toURL());
  try(URLClassLoader cl=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader());JarFile jar=new JarFile(args[0])) {
   Class<?> t=cl.loadClass("dev.replayconverter.viaversion.ProtocolTranslator");
   Class<?> via=cl.loadClass("com.viaversion.viaversion.api.Via");
   if((boolean)via.getMethod("isLoaded").invoke(null))throw new AssertionError("Via already loaded");
   for(int i=0;i<3;i++) {
    Object o=t.getConstructor(Path.class,int.class,int.class).newInstance(null,767,767);
    byte[] p={0x19,1,2,3};if(t.getMethod("translateClientbound",byte[].class).invoke(o,(Object)p)!=p)throw new AssertionError();
    if(!((List<?>)t.getMethod("drainInjectedPackets").invoke(o)).isEmpty())throw new AssertionError();
    if((boolean)t.getMethod("finishSyntheticConfigurationIfNeeded").invoke(o))throw new AssertionError();
    if(!t.getMethod("clientState").invoke(o).toString().equals("PLAY"))throw new AssertionError();
    System.out.println("Cycle "+(i+1)+": "+t.getMethod("states").invoke(o));
    for(String field:List.of("channel","connection")){var f=t.getDeclaredField(field);f.setAccessible(true);if(f.get(o)!=null)throw new AssertionError(field);}
    t.getMethod("close").invoke(o);t.getMethod("close").invoke(o);
    if((boolean)via.getMethod("isLoaded").invoke(null))throw new AssertionError("Native path initialized Via");
   }
   Class<?> converter=cl.loadClass("dev.replayconverter.ReplayConverter");
   var compact=converter.getDeclaredMethod("compactSnapshotGamePackets",List.class);compact.setAccessible(true);
   List<byte[]> packets=List.of(new byte[]{0x60,1,2},new byte[]{0x60,1,3},new byte[]{0x42,1});
   List<?> preserved=(List<?>)compact.invoke(null,packets);
   if(!preserved.equals(packets)||preserved==packets)throw new AssertionError("Native packet order/content changed");
   for(String name:List.of("dev/replayconverter/viaversion/ProtocolTranslator","dev/replayconverter/ReplayConverter")) {
    cl.loadClass(name.replace('/','.')).getDeclaredMethods();
    StringWriter errors=new StringWriter();CheckClassAdapter.verify(new ClassReader(jar.getInputStream(jar.getJarEntry(name+".class"))),cl,false,new PrintWriter(errors));
    if(!errors.toString().isBlank())throw new AssertionError(errors.toString());
   }
   System.out.println("PASS: strict JVM verification, ASM verification of both classes, three native conversion lifecycles with null config path, no Via initialization or Netty channel, unchanged payload, repeated close.");
  }
 }
}
