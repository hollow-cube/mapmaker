package net.hollowcube.terraform.task;

@FunctionalInterface
public interface PostApplyFunc {

    void exec(TaskResult result);

}
