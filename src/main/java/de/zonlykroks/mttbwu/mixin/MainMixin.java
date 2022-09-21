package de.zonlykroks.mttbwu.mixin;

import com.google.gson.Gson;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.glfw.WindowSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferRenderer;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.main.Main;
import net.minecraft.client.util.GlException;
import net.minecraft.client.util.Session;
import net.minecraft.util.WinNativeModuleLister;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.net.Proxy;
import java.util.List;
import java.util.OptionalInt;

@Mixin(Main.class)
public class MainMixin implements de.zonlykroks.mttbwu.extensions.IMain {

	@Shadow
	@Final
	private static Logger LOGGER;
	@Unique
	private static Thread renderingThread;

	@Inject(method = "main([Ljava/lang/String;Z)V", at = @At(value = "INVOKE", target = "Ljava/lang/Runtime;addShutdownHook(Ljava/lang/Thread;)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
	private static void injectThread(String args[], boolean bl, CallbackInfo ci, OptionParser optionParser, OptionSpec optionSpec, OptionSpec optionSpec2, OptionSpec optionSpec3, OptionSpec optionSpec4, OptionSpec optionSpec5, OptionSpec optionSpec6, OptionSpec optionSpec7, OptionSpec optionSpec8, OptionSpec optionSpec9, OptionSpec optionSpec10, OptionSpec optionSpec11, OptionSpec optionSpec12, OptionSpec optionSpec13, OptionSpec optionSpec14, OptionSpec optionSpec15, OptionSpec optionSpec16, OptionSpec optionSpec17, OptionSpec optionSpec18, OptionSpec optionSpec19, OptionSpec optionSpec20, OptionSpec optionSpec21, OptionSpec optionSpec22, OptionSpec optionSpec23, OptionSpec optionSpec24, OptionSpec optionSpec25, OptionSpec optionSpec26, OptionSet optionSet, List list, String string, Proxy proxy, String string2, String string3, int i, int j, OptionalInt optionalInt, OptionalInt optionalInt2, boolean bl2, boolean bl3, boolean bl4, boolean bl5, String string4, Gson gson, PropertyMap propertyMap, PropertyMap propertyMap2, String string5, File file, File file2, File file3, String string6, String string7, String string8, String string9, String string10, Integer integer, String string11, Session.AccountType accountType, Session session, RunArgs runArgs, Thread thread) {
		Thread renderThread = new Thread() {
			@Override
			public void run() {
				super.run();
			}
		};
		renderingThread = renderThread;
		Runtime.getRuntime().addShutdownHook(thread);

		final MinecraftClient minecraftClient;
		try {
			renderThread.setName("Render Thread");
			RenderSystem.initRenderThread();
			RenderSystem.beginInitialization();
			minecraftClient = new MinecraftClient(runArgs);
			RenderSystem.finishInitialization();
		} catch (GlException var76) {
			LOGGER.warn("Failed to create window: ", var76);
			return;
		} catch (Throwable var77) {
			CrashReport crashReport = CrashReport.create(var77, "Initializing game");
			CrashReportSection crashReportSection = crashReport.addElement("Initialization");
			WinNativeModuleLister.addReportSection(crashReportSection);
			MinecraftClient.addSystemDetailsToCrashReport(null, null, runArgs.game.version, null, crashReport);
			MinecraftClient.printCrashReport(crashReport);
			return;
		}

		Thread thread2;
		if (minecraftClient.shouldRenderAsync()) {
			thread2 = new Thread("Game thread") {
				public void run() {
					try {
						RenderSystem.initGameThread(true);
						minecraftClient.run();
					} catch (Throwable var2) {
						Main.LOGGER.error("Exception in client thread", var2);
					}

				}
			};
			thread2.start();

			while(minecraftClient.isRunning()) {
			}
		} else {
			thread2 = null;

			try {
				RenderSystem.initGameThread(false);
				minecraftClient.run();
			} catch (Throwable var75) {
				LOGGER.error("Unhandled game exception", var75);
			}
		}

		BufferRenderer.unbindAll();

		try {
			minecraftClient.scheduleStop();
			if (thread2 != null) {
				thread2.join();
			}
		} catch (InterruptedException var73) {
			LOGGER.error("Exception during client thread shutdown", var73);
		} finally {
			minecraftClient.stop();
		}
		ci.cancel();
	}

	@Override
	public Thread getRenderingThread() {
		return renderingThread;
	}
}
