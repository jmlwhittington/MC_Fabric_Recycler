package com.recycler;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RecyclerScreen extends AbstractContainerScreen<RecyclerMenu> {

    private static final Identifier SLOT_SPRITE =
            Identifier.withDefaultNamespace("container/slot");

    public RecyclerScreen(
            RecyclerMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title, 176, 166);

        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(
                x, y,
                x + this.imageWidth,
                y + this.imageHeight,
                0xFFC6C6C6
        );

        graphics.outline(
                x, y,
                this.imageWidth,
                this.imageHeight,
                0xFF404040
        );

		graphics.fill(
				x + 60, y + 38,
				x + 100, y + 45,
				0xFF555555
		);


		if (this.menu.isInputRejected()) {
			graphics.fill(
					x + 61, y + 39,
					x + 99, y + 45,
					0xFFAA3333
			);
		} else {
			int progressWidth = Math.round(38 * this.menu.getProgress());

			if (progressWidth > 0) {
				graphics.fill(
						x + 61, y + 39,
						x + 61 + progressWidth, y + 45,
						0xFF55AA55
				);
			}
		}

		graphics.outline(
				x + 60, y + 38,
				40, 8,
				0xFF404040
		);

		drawSlot(graphics, x + 26, y + 34);

		for (int row = 0; row < 2; row++) {
			for (int col = 0; col < 3; col++) {
				drawSlot(
						graphics,
						x + 116 + col * 18,
						y + 30 + row * 18
				);
			}
		}

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(
                        graphics,
                        x + 8 + col * 18,
                        y + 84 + row * 18
                );
            }
        }

        for (int col = 0; col < 9; col++) {
            drawSlot(
                    graphics,
                    x + 8 + col * 18,
                    y + 142
            );
        }
    }

    private void drawSlot(
            GuiGraphicsExtractor graphics,
            int x,
            int y
    ) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SLOT_SPRITE,
                x - 1,
                y - 1,
                18,
                18
        );
    }

	@Override
	protected void extractLabels(
			GuiGraphicsExtractor graphics,
			int mouseX,
			int mouseY
	) {
		int textColor = 0xFF404040;

		graphics.text(
				this.font,
				this.title,
				this.titleLabelX,
				this.titleLabelY,
				textColor,
				false
		);

		graphics.text(
				this.font,
				Component.translatable("container.inventory"),
				8,
				this.inventoryLabelY,
				textColor,
				false
		);

		graphics.text(
				this.font,
				"Input",
				21,
				23,
				textColor,
				false
		);

		String outputLabel = "Output";

		graphics.text(
				this.font,
				outputLabel,
				127,
				19,
				textColor,
				false
		);
	}
}
