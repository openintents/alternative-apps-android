package org.openintents.alternativeapps.intents.model;

import android.content.Intent;

import java.util.Arrays;
import java.util.List;

public class IntentSpecification {
    public static final List<IntentSpecification> POPULAR = Arrays.asList(new IntentSpecification[]{
            new IntentSpecification(Intent.ACTION_SEND, "Share"),
            new IntentSpecification(Intent.ACTION_ASSIST, "Help me"),
            new IntentSpecification(Intent.ACTION_BUG_REPORT, "Report a bug")
    });
    public String action;
    public String title;

    public IntentSpecification() {

    }

    public IntentSpecification(String action, String title) {
        this.action = action;
        this.title = title;
    }

    public Intent asIntent() {
        return new Intent(action);
    }
}
