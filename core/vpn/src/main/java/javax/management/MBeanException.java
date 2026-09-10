package javax.management;

/**
 * Android stub for the JDK's javax.management.MBeanException.
 *
 * <p>The Apache MINA SSHD library references this class in
 * {@code ExceptionUtils.peelException()} (and in bytecode it is linked even when
 * the {@code !OsUtils.isAndroid()} guard would skip it at runtime, because the
 * verifier resolves the reference when the method is compiled). The class does
 * not exist on Android, which crashed with
 * {@code NoClassDefFoundError: javax/management/MBeanException}. Define a
 * minimal API-compatible stub so the class resolves; it is never instantiated
 * on Android since JMX is not available.</p>
 */
public class MBeanException extends Exception {

    private final Exception exception;

    public MBeanException(Exception e) {
        super(e.toString());
        this.exception = e;
    }

    public MBeanException(Exception e, String message) {
        super(message);
        this.exception = e;
    }

    public Exception getTargetException() {
        return exception;
    }

    @Override
    public Throwable getCause() {
        return exception;
    }
}