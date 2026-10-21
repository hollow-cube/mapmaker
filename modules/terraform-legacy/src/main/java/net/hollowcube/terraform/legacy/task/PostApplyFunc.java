package net.hollowcube.terraform.legacy.task;

@FunctionalInterface
public interface PostApplyFunc {

    void exec(TaskResult result);

}
