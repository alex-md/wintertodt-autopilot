package com.wintertodtautopilot;

import java.awt.*;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

/** Larger wrapped instruction; its reported height keeps supporting rows below it. */
final class WintertodtInstructionComponent implements LayoutableRenderableEntity
{
    private final String text;
    private final Color color;
    @Setter private Point preferredLocation = new Point();
    @Setter private Dimension preferredSize = new Dimension(260, 0);
    @Getter private final Rectangle bounds = new Rectangle();
    WintertodtInstructionComponent(String text, Color color) { this.text = text; this.color = color; }
    @Override
    public Dimension render(Graphics2D graphics)
    {
        Font old = graphics.getFont();
        graphics.setFont(old.deriveFont(Font.BOLD, old.getSize2D() + 2));
        try
        {
            FontMetrics metrics = graphics.getFontMetrics();
            int y = preferredLocation.y;
            String line = "";
            for (String word : text.split(" "))
            {
                String next = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && metrics.stringWidth(next) > preferredSize.width)
                {
                    y += metrics.getHeight(); draw(graphics, line, y); line = word;
                }
                else { line = next; }
            }
            y += metrics.getHeight(); draw(graphics, line, y);
            Dimension size = new Dimension(preferredSize.width, y - preferredLocation.y + 3);
            bounds.setLocation(preferredLocation); bounds.setSize(size);
            return size;
        }
        finally { graphics.setFont(old); }
    }
    private void draw(Graphics2D graphics, String line, int y)
    {
        TextComponent component = new TextComponent();
        component.setText(line); component.setColor(color);
        component.setPosition(new Point(preferredLocation.x, y)); component.render(graphics);
    }
}
