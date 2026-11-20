package com.recycler;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RecyclerScreen extends AbstractContainerScreen<RecyclerMenu> {

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
                x,
                y,
                x + this.imageWidth,
                y + this.imageHeight,
                0xFF383838
        );

        graphics.outline(
                x,
                y,
                this.imageWidth,
                this.imageHeight,
                0xFF101010
        );

        graphics.fill(
                x + 8,
                y + 17,
                x + 168,
                y + 67,
                0xFF484848
        );

        graphics.outline(
                x + 8,
                y + 17,
                160,
                50,
                0xFF202020
        );

        graphics.fill(
                x + 63,
                y + 39,
                x + 103,
                y + 47,
                0xFF202020
        );
		
		int progressWidth = Math.round(38 * this.menu.getProgress());

		if (progressWidth > 0) {
			graphics.fill(
					x + 64,
					y + 40,
					x + 64 + progressWidth,
					y + 46,
					0xFF55CC55
			);
		}

        graphics.outline(
                x + 63,
                y + 39,
                40,
                8,
                0xFF101010
        );

        graphics.fill(
                x + 7,
                y + 80,
                x + 169,
                y + 162,
                0xFF484848
        );

        graphics.outline(
                x + 7,
                y + 80,
                162,
                82,
                0xFF202020
        );
		
		drawSlot(graphics, x + 26, y + 35);

		for (int row = 0; row < 2; row++) {
			for (int col = 0; col < 3; col++) {
				drawSlot(
						graphics,
						x + 112 + col * 18,
						y + 26 + row * 18
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

	private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(
				x, y,
				x + 18, y + 18,
				0xFF202020
		);

		graphics.fill(
				x + 1, y + 1,
				x + 17, y + 17,
				0xFF777777
		);
	}

	@Override
	protected void extractLabels(
			GuiGraphicsExtractor graphics,
			int mouseX,
			int mouseY
	) {
		graphics.text(
				this.font,
				this.title,
				this.titleLabelX,
				this.titleLabelY,
				0xFFFFFFFF
		);

		graphics.text(
				this.font,
				Component.translatable("container.inventory"),
				8,
				this.inventoryLabelY,
				0xFFFFFFFF
		);

		graphics.text(
				this.font,
				"Input",
				24,
				21,
				0xFFFFFFFF
		);

		graphics.text(
				this.font,
				"Progress",
				63,
				27,
				0xFFFFFFFF
		);

		graphics.text(
				this.font,
				"Output",
				112,
				12,
				0xFFFFFFFF
		);

		graphics.text(
				this.font,
				this.menu.getStatusText(),
				63,
				51,
				0xFFFFFFFF
		);
	}
}
