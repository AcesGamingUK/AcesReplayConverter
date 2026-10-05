import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.net.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class FixNative implements Opcodes {
 static final String OWNER="dev/replayconverter/viaversion/ProtocolTranslator";
 static InsnList guard(LabelNode normal) {
  InsnList x=new InsnList();x.add(new VarInsnNode(ALOAD,0));
  x.add(new FieldInsnNode(GETFIELD,OWNER,"native767","Z"));x.add(new JumpInsnNode(IFEQ,normal));return x;
 }
 public static void main(String[] args) throws Exception {
  List<URL> urls=new ArrayList<>();for(int i=0;i<args.length;i++)if(i!=1)urls.add(Path.of(args[i]).toUri().toURL());
  try(URLClassLoader loader=new URLClassLoader(urls.toArray(URL[]::new),FixNative.class.getClassLoader());JarFile in=new JarFile(args[0]);JarOutputStream out=new JarOutputStream(Files.newOutputStream(Path.of(args[1])))) {
   for(JarEntry e:Collections.list(in.entries())) {
    byte[] b=in.getInputStream(e).readAllBytes();
    if(e.getName().equals(OWNER+".class")) {
     ClassNode c=new ClassNode();new ClassReader(b).accept(c,ClassReader.SKIP_FRAMES);
     c.fields.add(new FieldNode(ACC_PRIVATE|ACC_FINAL,"native767","Z",null,null));
     int changed=0;
     for(MethodNode m:c.methods) {
      if(m.name.equals("<init>")) {
       AbstractInsnNode superCall=m.instructions.getFirst();
       while(!(superCall instanceof MethodInsnNode n && n.owner.equals("java/lang/Object") && n.name.equals("<init>")))superCall=superCall.getNext();
       LabelNode normal=new LabelNode();InsnList x=new InsnList();
       x.add(new VarInsnNode(ILOAD,2));x.add(new IntInsnNode(SIPUSH,767));x.add(new JumpInsnNode(IF_ICMPNE,normal));
       x.add(new VarInsnNode(ILOAD,3));x.add(new IntInsnNode(SIPUSH,767));x.add(new JumpInsnNode(IF_ICMPNE,normal));
       x.add(new VarInsnNode(ALOAD,0));x.add(new InsnNode(ICONST_1));x.add(new FieldInsnNode(PUTFIELD,OWNER,"native767","Z"));
       x.add(new VarInsnNode(ALOAD,0));x.add(new InsnNode(ACONST_NULL));x.add(new FieldInsnNode(PUTFIELD,OWNER,"channel","Lio/netty/channel/embedded/EmbeddedChannel;"));
       x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,"java/util/Collections","emptyList","()Ljava/util/List;",false));x.add(new FieldInsnNode(PUTFIELD,OWNER,"injectedPackets","Ljava/util/List;"));
       x.add(new InsnNode(RETURN));x.add(normal);m.instructions.insert(superCall,x);changed++;continue;
      }
      if(!Set.of("translateClientbound","drainInjectedPackets","states","clientState","finishSyntheticConfigurationIfNeeded","close").contains(m.name))continue;
      LabelNode normal=new LabelNode();InsnList x=guard(normal);
      switch(m.name) {
       case "translateClientbound" -> {x.add(new VarInsnNode(ALOAD,1));x.add(new InsnNode(ARETURN));}
       case "drainInjectedPackets" -> {x.add(new MethodInsnNode(INVOKESTATIC,"java/util/Collections","emptyList","()Ljava/util/List;",false));x.add(new InsnNode(ARETURN));}
       case "states" -> {x.add(new LdcInsnNode("PLAY -> PLAY (native 767)"));x.add(new InsnNode(ARETURN));}
       case "clientState" -> {x.add(new FieldInsnNode(GETSTATIC,"com/viaversion/viaversion/api/protocol/packet/State","PLAY","Lcom/viaversion/viaversion/api/protocol/packet/State;"));x.add(new InsnNode(ARETURN));}
       case "finishSyntheticConfigurationIfNeeded" -> {x.add(new InsnNode(ICONST_0));x.add(new InsnNode(IRETURN));}
       case "close" -> x.add(new InsnNode(RETURN));
      }
      x.add(normal);m.instructions.insert(x);changed++;
     }
     if(changed!=7)throw new AssertionError(changed);
     ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS) {
      @Override protected ClassLoader getClassLoader(){return loader;}
     };
     c.accept(cw);b=cw.toByteArray();Files.createDirectories(Path.of("work/native-fixed"));Files.write(Path.of("work/native-fixed/ProtocolTranslator.class"),b);
    }
    if(e.getName().equals("dev/replayconverter/ReplayConverter.class")) {
     ClassReader cr=new ClassReader(b);ClassWriter cw=new ClassWriter(cr,ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
     int[] replaced={0};
     cr.accept(new ClassVisitor(ASM9,cw) {
      @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions) {
       MethodVisitor mv=super.visitMethod(access,name,desc,sig,exceptions);
       if(!name.equals("compactSnapshotGamePackets"))return mv;
       if(!desc.equals("(Ljava/util/List;)Ljava/util/List;"))throw new AssertionError(desc);
       // Preserve native packet order and every equipment update. The previous
       // merger decodes modded 1.21.1 item stacks using Via's 1.20.5 codec.
       replaced[0]++;mv.visitCode();mv.visitTypeInsn(NEW,"java/util/ArrayList");mv.visitInsn(DUP);mv.visitVarInsn(ALOAD,0);
       mv.visitMethodInsn(INVOKESPECIAL,"java/util/ArrayList","<init>","(Ljava/util/Collection;)V",false);mv.visitInsn(ARETURN);mv.visitMaxs(0,0);mv.visitEnd();return null;
      }
     },0);
     if(replaced[0]!=1)throw new AssertionError();b=cw.toByteArray();
    }
    if(e.getName().equals("fabric.mod.json")) {
     String s=new String(b,java.nio.charset.StandardCharsets.UTF_8);if(!s.contains("\"version\":\"0.1.3-dev\""))throw new AssertionError();
     b=s.replace("\"version\":\"0.1.3-dev\"","\"version\":\"0.1.4-dev\"").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    JarEntry n=new JarEntry(e.getName());n.setTime(e.getTime());out.putNextEntry(n);out.write(b);out.closeEntry();
   }
  }
  System.out.println("Built 0.1.4-dev with ASM-computed frames for all modified methods.");
 }
}
