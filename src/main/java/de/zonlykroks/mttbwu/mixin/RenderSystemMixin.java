package de.zonlykroks.mttbwu.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import de.zonlykroks.mttbwu.extensions.IMain;
import net.minecraft.client.main.Main;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
  @Shadow
  @Nullable
  private static Thread renderThread;

  @Shadow
  @Nullable
  private static Thread gameThread;

  @Overwrite(remap = false)
  public static void initRenderThread() {
	Main main = new Main();
    if (renderThread == null && gameThread != Thread.currentThread()) {
      renderThread = ((IMain)main).getRenderingThread();
    } else {
      throw new IllegalStateException("Could not initialize render thread");
    }
  }

  @Overwrite(remap = false)
  public static boolean isOnRenderThread() {
    return true;
  }

  @Overwrite(remap = false)
  public static void initGameThread(boolean bl) {
    if (gameThread == null && renderThread != null) {
      gameThread = Thread.currentThread();
    } else {
      throw new IllegalStateException("Could not initialize tick thread");
    }
  }
}
