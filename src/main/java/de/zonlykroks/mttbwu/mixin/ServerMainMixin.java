package de.zonlykroks.mttbwu.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import de.zonlykroks.mttbwu.MayTheThreadsBeWithYou;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.fabricmc.fabric.impl.gametest.FabricGameTestHelper;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.world.GeneratorTypes;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.pack.*;
import net.minecraft.server.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.dedicated.EulaReader;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.ServerPropertiesHandler;
import net.minecraft.server.dedicated.ServerPropertiesLoader;
import net.minecraft.text.Text;
import net.minecraft.unmapped.C_kjxfcecs;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.RecordingSide;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryOps;
import net.minecraft.world.GameRules;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.updater.WorldUpdater;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.server.DedicatedServerModInitializer;
import org.quiltmc.qsl.resource.loader.api.ResourceLoaderEvents;
import org.quiltmc.qsl.resource.loader.impl.ModResourcePackUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

@Mixin(Main.class)
public abstract class ServerMainMixin {
	@Final
	@Shadow
	private static Logger LOGGER;

	@Shadow
	private static void forceUpgradeWorld(LevelStorage.Session session, DataFixer dataFixer, boolean eraseCache, BooleanSupplier shouldContinue, GeneratorOptions generatorOptions) {
	}

	@Inject(method = "main", at = @At(value = "HEAD"), cancellable = true, remap = false)
	private static void main(String[] strings, CallbackInfo ci) {
		MayTheThreadsBeWithYou.LOGGER.info("Starting Minecraft Server");
		SharedConstants.createGameVersion();
		OptionParser optionParser = new OptionParser();
		OptionSpec<Void> optionSpec = optionParser.accepts("nogui");
		OptionSpec<Void> optionSpec2 = optionParser.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
		OptionSpec<Void> optionSpec3 = optionParser.accepts("demo");
		OptionSpec<Void> optionSpec4 = optionParser.accepts("bonusChest");
		OptionSpec<Void> optionSpec5 = optionParser.accepts("forceUpgrade");
		OptionSpec<Void> optionSpec6 = optionParser.accepts("eraseCache");
		OptionSpec<Void> optionSpec7 = optionParser.accepts("safeMode", "Loads level with vanilla datapack only");
		OptionSpec<Void> optionSpec8 = optionParser.accepts("help").forHelp();
		OptionSpec<String> optionSpec9 = optionParser.accepts("singleplayer").withRequiredArg();
		OptionSpec<String> optionSpec10 = optionParser.accepts("universe").withRequiredArg().defaultsTo(".", new String[0]);
		OptionSpec<String> optionSpec11 = optionParser.accepts("world").withRequiredArg();
		OptionSpec<Integer> optionSpec12 = optionParser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1, new Integer[0]);
		OptionSpec<String> optionSpec13 = optionParser.accepts("serverId").withRequiredArg();
		OptionSpec<Void> optionSpec14 = optionParser.accepts("jfrProfile");
		OptionSpec<String> optionSpec15 = optionParser.nonOptions();

		try {
			OptionSet optionSet = optionParser.parse(strings);
			if (optionSet.has(optionSpec8)) {
				optionParser.printHelpOn(System.err);
				return;
			}

			CrashReport.initCrashReport();
			if (optionSet.has(optionSpec14)) {
				JvmProfiler.INSTANCE.start(RecordingSide.SERVER);
			}

			Bootstrap.initialize();
			Bootstrap.logMissing();
			Util.startTimerHack();
			Path path = Paths.get("server.properties");
			if (FabricDataGenHelper.ENABLED) {
				FabricDataGenHelper.run();
				return;
			}
			ServerPropertiesLoader serverPropertiesLoader = new ServerPropertiesLoader(path);
			serverPropertiesLoader.store();
			Path path2 = Paths.get("eula.txt");
			EulaReader eulaReader = new EulaReader(path2);
			if (optionSet.has(optionSpec2)) {
				LOGGER.info("Initialized '{}' and '{}'", path.toAbsolutePath(), path2.toAbsolutePath());
				return;
			}

			boolean eula = FabricGameTestHelper.ENABLED || eulaReader.isEulaAgreedTo();
			if (!eula) {
				LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
				return;
			}

			for (var initializer : QuiltLoader.getEntrypointContainers(DedicatedServerModInitializer.ENTRYPOINT_KEY, DedicatedServerModInitializer.class)) {
				initializer.getEntrypoint().onInitializeServer(initializer.getProvider());
			}
			File file = new File(optionSet.valueOf(optionSpec10));
			Services services = Services.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), file);
			String string = Optional.ofNullable(optionSet.valueOf(optionSpec11)).orElse(serverPropertiesLoader.getPropertiesHandler().levelName);
			LevelStorage levelStorage = LevelStorage.create(file.toPath());
			LevelStorage.Session session = levelStorage.createSession(string);
			LevelSummary levelSummary = session.getLevelSummary();
			if (levelSummary != null) {
				if (levelSummary.doesRequireConversion()) {
					LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
					return;
				}

				if (!levelSummary.canBeLoaded()) {
					LOGGER.info("This world was created by an incompatible version.");
					return;
				}
			}

			boolean bl = optionSet.has(optionSpec7);
			if (bl) {
				LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
			}

			ResourcePackManager resourcePackManager = new ResourcePackManager(ResourceType.SERVER_DATA, new VanillaDataPackProvider(), new FileResourcePackProvider(session.getDirectory(WorldSavePath.DATAPACKS).toFile(), ResourcePackSource.PACK_SOURCE_WORLD));
			if (FabricGameTestHelper.ENABLED) {
				FabricGameTestHelper.runHeadlessServer(session, resourcePackManager);
				return;  // Do not progress in starting the normal dedicated server
			}

			Thread worldThread = new Thread(() -> {
				WorldStem worldStem;
				try {
					DataPackSettings dataPackSettings = Objects.requireNonNullElse(session.getDataPackSettings(), ModResourcePackUtil.DEFAULT_SETTINGS);
					C_kjxfcecs.C_nrmvgbka c_nrmvgbka = new C_kjxfcecs.C_nrmvgbka(resourcePackManager, dataPackSettings, bl);
					C_kjxfcecs.C_kculhjuh c_kculhjuh = new C_kjxfcecs.C_kculhjuh(c_nrmvgbka, CommandManager.RegistrationEnvironment.DEDICATED, serverPropertiesLoader.getPropertiesHandler().functionPermissionLevel);
					ResourceLoaderEvents.START_DATA_PACK_RELOAD.invoker().onStartDataPackReload(null, null);
					worldStem = Util.method_43499((executor) -> WorldStem.load(c_kculhjuh, (resourceManager, dataPackSettings1) -> {
						DynamicRegistryManager.Writable writable = DynamicRegistryManager.builtInCopy();
						DynamicOps<NbtElement> dynamicOps = RegistryOps.createAndLoad(NbtOps.INSTANCE, writable, resourceManager);
						SaveProperties saveProperties = session.readLevelProperties(dynamicOps, dataPackSettings1, writable.allElementsLifecycle());
						if (saveProperties != null) {
							return Pair.of(saveProperties, writable.freeze());
						} else {
							LevelInfo levelInfo;
							GeneratorOptions generatorOptions;
							if (optionSet.has(optionSpec3)) {
								levelInfo = MinecraftServer.DEMO_LEVEL_INFO;
								generatorOptions = GeneratorTypes.createDemo(writable);
							} else {
								ServerPropertiesHandler serverPropertiesHandler = serverPropertiesLoader.getPropertiesHandler();
								levelInfo = new LevelInfo(serverPropertiesHandler.levelName, serverPropertiesHandler.gameMode, serverPropertiesHandler.hardcore, serverPropertiesHandler.difficulty, false, new GameRules(), dataPackSettings1);
								generatorOptions = optionSet.has(optionSpec4) ? serverPropertiesHandler.getGeneratorOptions(writable).withBonusChest() : serverPropertiesHandler.getGeneratorOptions(writable);
							}

							LevelProperties levelProperties = new LevelProperties(levelInfo, generatorOptions, Lifecycle.stable());
							return Pair.of(levelProperties, writable.freeze());
						}
					}, Util.getMainWorkerExecutor(), executor)).get();
					ResourceLoaderEvents.END_DATA_PACK_RELOAD.invoker().onEndDataPackReload(null, worldStem.resourceManager(), null);
				} catch (Exception exception) {
					ResourceLoaderEvents.END_DATA_PACK_RELOAD.invoker().onEndDataPackReload(null, null, exception);
					LOGGER.warn("Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", exception);
					return;
				}

				DynamicRegistryManager.Frozen frozen = worldStem.registryManager();
				serverPropertiesLoader.getPropertiesHandler().getGeneratorOptions(frozen);
				SaveProperties saveProperties = worldStem.saveProperties();
				if (optionSet.has(optionSpec5)) {
					forceUpgradeWorld(session, Schemas.getFixer(), optionSet.has(optionSpec6), () -> true, saveProperties.getGeneratorOptions());
				}

				session.backupLevelDataFile(frozen, saveProperties);
				final MinecraftDedicatedServer dedicatedServer = MinecraftServer.startServer((threadx) -> {
					MinecraftDedicatedServer minecraftDedicatedServer = new MinecraftDedicatedServer(threadx, session, resourcePackManager, worldStem, serverPropertiesLoader, Schemas.getFixer(), services, WorldGenerationProgressLogger::new);
					minecraftDedicatedServer.setHostProfile(optionSet.has(optionSpec9) ? new GameProfile(null, optionSet.valueOf(optionSpec9)) : null);
					minecraftDedicatedServer.setServerPort(optionSet.valueOf(optionSpec12));
					minecraftDedicatedServer.setDemo(optionSet.has(optionSpec3));
					minecraftDedicatedServer.setServerId(optionSet.valueOf(optionSpec13));
					boolean bl1 = !optionSet.has(optionSpec) && !optionSet.valuesOf(optionSpec15).contains("nogui");
					if (bl1 && !GraphicsEnvironment.isHeadless()) {
						minecraftDedicatedServer.createGui();
					}

					return minecraftDedicatedServer;
				});
				Thread thread = new Thread("Server Shutdown Thread") {
					public void run() {
						dedicatedServer.stop(true);
					}
				};
				thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
				Runtime.getRuntime().addShutdownHook(thread);
			});
			worldThread.start();
		} catch (Exception var36) {
			LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", var36);
			if (FabricGameTestHelper.ENABLED) {
				System.exit(-1);
			}
		}
		ci.cancel();
	}
}
