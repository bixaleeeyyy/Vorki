package com.vorki.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vorki.app.core.Ui;
import com.vorki.app.core.VorkiActivity;

public class MainActivity extends VorkiActivity {

    private static final int[] GRADIENTS = {
            R.drawable.card_g1, R.drawable.card_g2, R.drawable.card_g3, R.drawable.card_g4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ui.applyInsets(findViewById(R.id.screen_root));

        findViewById(R.id.btn_settings).setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout cards = findViewById(R.id.cards);
        LayoutInflater inflater = LayoutInflater.from(this);
        int position = 0; // keeps everything above the tip at the bottom
        int colour = 0;
        for (WidgetCatalog.Category category : WidgetCatalog.ALL) {
            TextView header = (TextView) inflater.inflate(R.layout.item_section, cards, false);
            header.setText(category.titleRes);
            cards.addView(header, position++);
            for (WidgetCatalog.Entry entry : category.entries) {
                View card = inflater.inflate(R.layout.item_widget_card, cards, false);
                card.findViewById(R.id.card_bg).setBackgroundResource(GRADIENTS[colour++ % GRADIENTS.length]);
                ((TextView) card.findViewById(R.id.card_title)).setText(entry.titleRes);
                ((TextView) card.findViewById(R.id.card_subtitle)).setText(entry.subtitleRes);
                ((ImageView) card.findViewById(R.id.card_icon)).setImageResource(entry.iconRes);
                card.setOnClickListener(v -> startActivity(new Intent(this, entry.screen)));
                cards.addView(card, position++);
            }
        }
    }
}
