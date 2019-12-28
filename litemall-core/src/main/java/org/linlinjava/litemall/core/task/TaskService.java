package org.linlinjava.litemall.core.task;

import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;

@Component
public class TaskService {
    private TaskService taskService;
    private DelayQueue<AbstractTask> delayQueue =  new DelayQueue<AbstractTask>();

    @PostConstruct
    private void init() {
        taskService = this;

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        AbstractTask task = delayQueue.take();
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void addTask(AbstractTask task){
        if(delayQueue.contains(task)){
            return;
        }
        delayQueue.add(task);
    }

    public void removeTask(AbstractTask task){
        delayQueue.remove(task);
    }

}
