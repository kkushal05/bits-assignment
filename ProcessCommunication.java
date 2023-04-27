import java.util.Random;
import java.util.concurrent.Semaphore;

/**
Note that the sendSignal() method should use platform-specific code to send the SIGTERM and SIGKILL signals. In this example, I've left it as a comment and used the Runtime.getRuntime().exec() method as an example for Linux. On other platforms, this code may need to be modified accordingly.
**/
public class ProcessCommunication {
    static int N;
    static int M;
    static Semaphore[] semaphores;
    static Random random;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java ProcessCommunication N M");
            System.exit(1);
        }

        N = Integer.parseInt(args[0]);
        M = Integer.parseInt(args[1]);
        semaphores = new Semaphore[N];
        random = new Random();

        for (int i = 0; i < N; i++) {
            semaphores[i] = new Semaphore(0);
            final int pid = i;
            Thread t = new Thread(() -> {
                if (pid % 2 == 0) {
                    evenProcess(pid);
                } else {
                    oddProcess(pid);
                }
            });
            t.start();
        }
    }

    private static void evenProcess(int pid) {
        int count = 0;
        while (true) {
            try {
                semaphores[pid].acquire();
                count++;
                System.out.println("Received signal " + count + " from " + semaphores[pid].availablePermits());
                if (count > M) {
                    int sender = semaphores[pid].availablePermits();
                    semaphores[pid].release(); // release the last acquired signal permit
                    sendSignal(pid, sender);
                    System.out.println("Sent SIGTERM and SIGKILL to " + sender);
                    System.out.println("Terminated Self");
                    System.exit(0);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void oddProcess(int pid) {
        while (true) {
            int receiver = random.nextInt(pid);
            semaphores[receiver].release(); // send a signal to a randomly chosen even process
            System.out.println("Sent signal to " + receiver);
            try {
                Thread.sleep(1000); // wait for 1 second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendSignal(int pid, int receiver) {
        // use platform-specific code to send SIGTERM and SIGKILL signals
        // for example, on Linux, use the following code:
        // Runtime.getRuntime().exec("kill -15 " + receiver);
        // Runtime.getRuntime().exec("kill -9 " + receiver);
    }
}
