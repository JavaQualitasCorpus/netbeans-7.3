/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.dlight.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.netbeans.modules.dlight.util.DLightExecutorService.DLightScheduledTask;
import org.openide.util.Exceptions;

/**
 *
 * @author ak119685
 */
public class TimerTaskExecutionServiceTest {

    final static AtomicInteger counterInvoked = new AtomicInteger();
    final static AtomicInteger counterCancelled = new AtomicInteger();
    private static volatile boolean countInvokations = false;

    public TimerTaskExecutionServiceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of registerTimerTask method, of class TimerTaskExecutionService.
     */
    @Ignore // Disabled because was very unstable
    @Test
    public void testRegisterTimerTask() {
        System.out.println("registerTimerTask");//NOI18N

        int count = 100;
        final List<Worker> workers = new ArrayList<Worker>();
        final List<DLightScheduledTask> tasks = Collections.synchronizedList(new ArrayList<DLightScheduledTask>());

        for (int i = 0; i < count; i++) {
            workers.add(new Worker());
        }

        final Random r = new Random();

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                int id = 0;
                for (Worker worker : workers) {
                    // will start this worker at fixed rate 
                    // (every 1, 2, 3, 4 or 5 seconds)
                    int rate = r.nextInt(4) + 1;

                    DLightScheduledTask task = DLightExecutorService.scheduleAtFixedRate(
                            worker, rate, TimeUnit.SECONDS, "testRegisterTimerTask " + (++id));
                    tasks.add(task);
                }
            }
        };

        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);

        t1.start();
        t2.start();

        try {
            t1.join();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }

        try {
            t2.join();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }

        // Threads creation takes a while - so some tasks may be already called
        // before others were not even scheduled yet... This will affect
        // statistics...

        countInvokations = true;

        /*
         * Will sleep for 5 seconds...
         * All our threads will be invoked with different interval.
         * Important thing is that this interval is 1 second MINIMUM.
         * So every task must be invoked <= 10 times (we used same workers twice!)
         */

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }

        // While iteration through all thread (to cancel) some timers can tick
        // again...
        // In this case workers may be invoked more than 10 times...
        // To prevent this will not count invokations after this point.

        countInvokations = false;

        for (DLightScheduledTask task : tasks) {
            task.cancel();
        }

        int size = 15;

        ArrayList<Integer> distribution = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            distribution.add(0);
        }

        for (Worker w : workers) {
            distribution.set(w.invokations, distribution.get(w.invokations) + 1);
        }

        System.out.println("Total tasks: " + count);
        System.out.println("Started tasks: " + counterInvoked);
        System.out.println("Cancelled tasks: " + counterCancelled);
        System.out.println("Distribution: " + Arrays.toString(distribution.toArray(new Integer[0])));

        assertTrue(counterInvoked.get() <= (count * 5 * 2));
        assertTrue(counterCancelled.get() <= count * 2); // Worker can be cancelled twice (from each service)
        assertTrue(distribution.get(0) == 0); // Each task should be invoked at least twice
        assertTrue(distribution.get(1) == 0);

        for (int i = 11; i < distribution.size(); i++) {
            // maximum possible invokations number is 10! (5 * 2)
            assertEquals(0, distribution.get(i).intValue());
        }

    }

    /**
     * Test of unregisterTimerTask method, of class TimerTaskExecutionService.
     */
//    @Test
    public void testUnregisterTimerTask() {
        System.out.println("unregisterTimerTask");//NOI18N
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");//NOI18N
    }

    private static class Worker implements Runnable {

        private int invokations = 0;

        @Override
        public void run() {

            if (countInvokations) {
                invokations++;
            }

            try {
                Thread.sleep(500);
                counterInvoked.incrementAndGet();
            } catch (InterruptedException ex) {
                counterCancelled.incrementAndGet();
            }
        }
    }
}