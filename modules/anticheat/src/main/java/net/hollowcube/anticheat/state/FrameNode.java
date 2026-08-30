package net.hollowcube.anticheat.state;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// One link of an accumulate key's frame chain, newest first.
///
/// Appending allocates a new head and leaves every existing node untouched, so a snapshot that
/// captured the old head can never see a later append and does not have to copy anything.
record FrameNode(StateFrame frame, @Nullable FrameNode previous) {

    static FrameNode append(@Nullable FrameNode head, StateFrame frame) {
        return new FrameNode(frame, head);
    }

    /// The chain in arrival order.
    List<StateFrame> toList() {
        var result = new ArrayList<StateFrame>();
        for (FrameNode node = this; node != null; node = node.previous) result.add(node.frame);
        Collections.reverse(result);
        return result;
    }
}
