import java.nio.file.*;
import java.net.*;
import java.util.*;
public class RunConvert {
 public static void main(String[] args)throws Exception {
  List<URL> urls=new ArrayList<>();urls.add(Path.of(args[0]).toUri().toURL());for(int i=3;i<args.length;i++)urls.add(Path.of(args[i]).toUri().toURL());
  try(URLClassLoader cl=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())) {
   Class<?> c=cl.loadClass("dev.replayconverter.ReplayConverter");
   Object result=c.getMethod("convert",Path.class,Path.class,java.util.function.Consumer.class).invoke(c.getConstructor().newInstance(),Path.of(args[1]),Path.of(args[2]),(java.util.function.Consumer<String>)s->{if(!s.startsWith("Translated"))System.out.println(s);});
   System.out.println(result);
   if((boolean)cl.loadClass("com.viaversion.viaversion.api.Via").getMethod("isLoaded").invoke(null))throw new AssertionError("Via loaded");
   System.out.println("PASS: real recording converted without initializing ViaVersion.");
  }
 }
}
