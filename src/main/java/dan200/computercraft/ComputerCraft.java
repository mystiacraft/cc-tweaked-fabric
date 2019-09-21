/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.client.proxy.ComputerCraftProxyClient;
import dan200.computercraft.core.apis.AddressPredicate;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import dan200.computercraft.core.filesystem.ResourceMount;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.core.ClientComputerRegistry;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.peripheral.diskdrive.BlockDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull;
import dan200.computercraft.shared.peripheral.modem.wired.ItemBlockCable;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor;
import dan200.computercraft.shared.peripheral.printer.BlockPrinter;
import dan200.computercraft.shared.peripheral.speaker.BlockSpeaker;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.proxy.ComputerCraftProxyCommon;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public final class ComputerCraft implements ModInitializer
{
    public static final String MOD_ID = "computercraft";

    public static final int DATAFIXER_VERSION = 0;

    // Configuration options
    public static final String[] DEFAULT_HTTP_WHITELIST = new String[] { "*" };
    public static final String[] DEFAULT_HTTP_BLACKLIST = new String[] {
        "127.0.0.0/8",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "fd00::/8",
    };

    public static int computerSpaceLimit = 1000 * 1000;
    public static int floppySpaceLimit = 125 * 1000;
    public static int maximumFilesOpen = 128;
    public static boolean disable_lua51_features = false;
    public static String default_computer_settings = "";
    public static boolean debug_enable = true;
    public static boolean logPeripheralErrors = false;

    public static int computer_threads = 1;
    public static long maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos( 10 );
    public static long maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos( 5 );

    public static boolean http_enable = true;
    public static boolean http_websocket_enable = true;
    public static AddressPredicate http_whitelist = new AddressPredicate( DEFAULT_HTTP_WHITELIST );
    public static AddressPredicate http_blacklist = new AddressPredicate( DEFAULT_HTTP_BLACKLIST );

    public static int httpTimeout = 30000;
    public static int httpMaxRequests = 16;
    public static long httpMaxDownload = 16 * 1024 * 1024;
    public static long httpMaxUpload = 4 * 1024 * 1024;
    public static int httpMaxWebsockets = 4;
    public static int httpMaxWebsocketMessage = Websocket.MAX_MESSAGE_SIZE;

    public static boolean enableCommandBlock = false;
    public static int modem_range = 64;
    public static int modem_highAltitudeRange = 384;
    public static int modem_rangeDuringStorm = 64;
    public static int modem_highAltitudeRangeDuringStorm = 384;
    public static int maxNotesPerTick = 8;

    public static boolean turtlesNeedFuel = true;
    public static int turtleFuelLimit = 20000;
    public static int advancedTurtleFuelLimit = 100000;
    public static boolean turtlesObeyBlockProtection = true;
    public static boolean turtlesCanPush = true;
    public static EnumSet<TurtleAction> turtleDisabledActions = EnumSet.noneOf( TurtleAction.class );

    public static final int terminalWidth_computer = 51;
    public static final int terminalHeight_computer = 19;

    public static final int terminalWidth_turtle = 39;
    public static final int terminalHeight_turtle = 13;

    public static final int terminalWidth_pocketComputer = 26;
    public static final int terminalHeight_pocketComputer = 20;

    // Blocks and Items
    public static final class Blocks
    {
        public static BlockComputer computerNormal;
        public static BlockComputer computerAdvanced;
        public static BlockComputer computerCommand;

        public static BlockTurtle turtleNormal;
        public static BlockTurtle turtleAdvanced;

        public static BlockSpeaker speaker;
        public static BlockDiskDrive diskDrive;
        public static BlockPrinter printer;

        public static BlockMonitor monitorNormal;
        public static BlockMonitor monitorAdvanced;

        public static BlockWirelessModem wirelessModemNormal;
        public static BlockWirelessModem wirelessModemAdvanced;

        public static BlockWiredModemFull wiredModemFull;
        public static BlockCable cable;
    }

    public static final class Items
    {
        public static ItemComputer computerNormal;
        public static ItemComputer computerAdvanced;
        public static ItemComputer computerCommand;

        public static ItemPocketComputer pocketComputerNormal;
        public static ItemPocketComputer pocketComputerAdvanced;

        public static ItemTurtle turtleNormal;
        public static ItemTurtle turtleAdvanced;

        public static ItemDisk disk;
        public static ItemTreasureDisk treasureDisk;

        public static ItemPrintout printedPage;
        public static ItemPrintout printedPages;
        public static ItemPrintout printedBook;

        public static ItemBlockCable.Cable cable;
        public static ItemBlockCable.WiredModem wiredModem;
    }

    public static final class TurtleUpgrades
    {
        public static TurtleModem wirelessModemNormal;
        public static TurtleModem wirelessModemAdvanced;
        public static TurtleSpeaker speaker;

        public static TurtleCraftingTable craftingTable;
        public static TurtleSword diamondSword;
        public static TurtleShovel diamondShovel;
        public static TurtleTool diamondPickaxe;
        public static TurtleAxe diamondAxe;
        public static TurtleHoe diamondHoe;
    }

    public static final class PocketUpgrades
    {
        public static PocketModem wirelessModemNormal;
        public static PocketModem wirelessModemAdvanced;
        public static PocketSpeaker speaker;
    }

    // Registries
    public static final ClientComputerRegistry clientComputerRegistry = new ClientComputerRegistry();
    public static final ServerComputerRegistry serverComputerRegistry = new ServerComputerRegistry();

    // Logging
    public static final Logger log = LogManager.getLogger( MOD_ID );

    // Implementation
    public static ComputerCraft instance;

    public ComputerCraft()
    {
        instance = this;
    }

    @Override
    public void onInitialize()
    {
        Config.load( Paths.get( FabricLoader.getInstance().getConfigDirectory().getPath(), MOD_ID + ".json5" ) );
        ComputerCraftProxyCommon.setup();
        if( FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT )
        {
            ComputerCraftProxyClient.setup();
        }
    }

    public static String getVersion()
    {
        return "${version}";
    }

    static IMount createResourceMount( String domain, String subPath )
    {
        ReloadableResourceManager manager = ComputerCraftProxyCommon.getServer().getDataManager();
        ResourceMount mount = new ResourceMount( domain, subPath, manager );
        return mount.exists( "" ) ? mount : null;
    }

    public static InputStream getResourceFile( String domain, String subPath )
    {
        ReloadableResourceManager manager = ComputerCraftProxyCommon.getServer().getDataManager();
        try
        {
            return manager.getResource( new Identifier( domain, subPath ) ).getInputStream();
        }
        catch( IOException ignored )
        {
            return null;
        }
    }
}
