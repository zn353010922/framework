/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leap.core.transaction;

import leap.core.annotation.Inject;
import leap.core.annotation.Transactional;
import leap.core.instrument.*;
import leap.lang.Try;
import leap.lang.asm.*;
import leap.lang.asm.commons.AdviceAdapter;
import leap.lang.asm.commons.Method;
import leap.lang.asm.tree.AnnotationNode;
import leap.lang.asm.tree.ClassNode;
import leap.lang.asm.tree.MethodNode;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;

import java.io.InputStream;
import java.util.Map;

public class TransactionInstrumentation extends AsmInstrumentProcessor implements AppInstrumentProcessor {

    private static final Log log = LogFactory.get(TransactionInstrumentation.class);

    private static final Type MANAGER_TYPE     = Type.getType(TransactionManager.class);
    private static final Type TRANS_TYPE       = Type.getType(Transactions.class);
    private static final Type INJECT_TYPE      = Type.getType(Inject.class);
    private static final Type PROPAGATION_TYPE = Type.getType(TransactionDefinition.Propagation.class);
    private static final Type TRAN_DEF_TYPE    = Type.getType(SimpleTransactionDefinition.class);

    private static final String MANAGER_FIELD    = "$$transactionManager";
    private static final String BEGIN_ALL        = "beginTransactionsAll";
    private static final String SET_ROLLBACK_ALL = "setRollbackAllOnly";
    private static final String COMPLETE_ALL     = "completeAll";

    private static Method TRAN_DEF_INIT_METHOD;
    private static Method TM_BEGIN_ALL_METHOD;

    static {
        Try.throwUnchecked(() -> {
            TRAN_DEF_INIT_METHOD = Method.getMethod(SimpleTransactionDefinition.class
                    .getConstructor(TransactionDefinition.Propagation.class));


            TM_BEGIN_ALL_METHOD = Method.getMethod(TransactionManager.class
                    .getMethod(BEGIN_ALL, TransactionDefinition.class));

        });
    }

    @Override
    protected void processClass(AppInstrumentContext context, AppInstrumentClass ic, ClassInfo ci, boolean methodBodyOnly) {
        ClassNode cn = ci.cn;
        if(null != cn.methods) {
            boolean hasTransactionalMethods = false;

            for(MethodNode mn : cn.methods) {
                if(ASM.isAnnotationPresent(mn, Transactional.class)) {
                    hasTransactionalMethods = true;
                    break;
                }
            }

            if(hasTransactionalMethods) {
                log.debug("Instrument Transactional class : {}", ic.getClassName());
                Try.throwUnchecked(() -> {
                    try(InputStream in = ci.is.getInputStream()) {
                        context.updateInstrumented(ic,
                                this,
                                instrumentClass(ci.cn, new ClassReader(in), true),
                                true);
                    }
                });
            }
        }
    }

    protected byte[] instrumentClass(ClassNode cn, ClassReader cr, boolean methodBodyOnly) {
        TxClassVisitor visitor = new TxClassVisitor(cn, new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES), methodBodyOnly);

        cr.accept(visitor, ClassReader.EXPAND_FRAMES);

        return visitor.getClassData();
    }

    protected static class TxClassVisitor extends ClassVisitor {

        private final ClassNode   cn;
        private final ClassWriter cw;
        private final Type        type;
        private final boolean     methodBodyOnly;

        public TxClassVisitor(ClassNode cn, ClassWriter cw, boolean methodBodyOnly) {
            super(ASM.API, cw);
            this.cn = cn;
            this.cw = cw;
            this.type = Type.getObjectType(cn.name);
            this.methodBodyOnly = methodBodyOnly;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodNode mn = ASM.getMethod(cn, name, desc);

            AnnotationNode a = ASM.getAnnotation(mn, Transactional.class);
            if(null != a) {
                log.trace(" #transactional method : {}", name);

                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                return new TxMethodVisitor(a, mv , access, name, desc);
            }

            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            if(!methodBodyOnly) {
                FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE, MANAGER_FIELD, MANAGER_TYPE.getDescriptor(), null, null);
                {
                    AnnotationVisitor av = fv.visitAnnotation(INJECT_TYPE.getDescriptor(), true);
                    av.visitEnd();
                }
                fv.visitEnd();
            }
            super.visitEnd();
        }

        public byte[] getClassData() {
            return cw.toByteArray();
        }

        protected class TxMethodVisitor extends AdviceAdapter {

            private final Label tryLabel     = new Label();
            private final Label finallyLabel = new Label();

            private final TransactionDefinition.Propagation propagation;

            private int transLocal = 0;

            protected TxMethodVisitor(AnnotationNode a, MethodVisitor mv, int access, String name, String desc) {
                super(ASM.API, mv, access, name, desc);

                Map<String,Object> values = ASM.getAnnotationValues(a);
                if(!values.isEmpty()) {
                    propagation = (TransactionDefinition.Propagation) values.get("propagation");
                }else{
                    propagation = TransactionDefinition.Propagation.REQUIRED;
                }
            }

            @Override
            public void visitCode() {
                super.visitCode();

                beginTransactions();
                visitLabel(tryLabel);
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                visitTryCatchBlock(tryLabel, finallyLabel, finallyLabel, null);
                visitLabel(finallyLabel);
                setRollbackTransactions();
                completeTransactions();
                visitInsn(Opcodes.ATHROW);

                super.visitMaxs(maxStack, maxLocals);
            }

            @Override
            protected void onMethodExit(int opcode) {
                if(opcode != Opcodes.ATHROW) {
                    completeTransactions();
                }
            }

            protected void newDefinition() {
                newInstance(TRAN_DEF_TYPE);
                dup();
                getStatic(PROPAGATION_TYPE, propagation.name(), PROPAGATION_TYPE);
                invokeConstructor(TRAN_DEF_TYPE, TRAN_DEF_INIT_METHOD);
            }

            protected void getTransactionManager() {
                if(!methodBodyOnly) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD,
                            type.getInternalName(),
                            MANAGER_FIELD,
                            MANAGER_TYPE.getDescriptor());
                }else{
                    invokeStatic(TxInst.TYPE,TxInst.MANAGER);
                }
            }

            protected void beginTransactions() {
                //get the instance of transaction manager.
                getTransactionManager();

                //create transaction definition.
                newDefinition();

                //invoke the begin transaction method.
                invokeInterface(MANAGER_TYPE, TM_BEGIN_ALL_METHOD);

                //store local variable.
                transLocal = newLocal(TRANS_TYPE);
                storeLocal(transLocal);
            }

            protected void setRollbackTransactions() {
                loadLocal(transLocal);
                mv.visitMethodInsn(INVOKEINTERFACE,
                        TRANS_TYPE.getInternalName(),
                        SET_ROLLBACK_ALL,
                        "()V", true);
            }

            protected void completeTransactions() {
                loadLocal(transLocal);
                mv.visitMethodInsn(INVOKEINTERFACE,
                        TRANS_TYPE.getInternalName(),
                        COMPLETE_ALL,
                        "()V", true);
            }
        }
    }


}