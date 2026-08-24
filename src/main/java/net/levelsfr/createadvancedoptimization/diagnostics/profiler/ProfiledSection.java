package net.levelsfr.createadvancedoptimization.diagnostics.profiler;

public enum ProfiledSection {
    SMART_BLOCK_ENTITY_TICKER_TICK(
        "Machine Tick Loop",
        "SmartBlockEntityTicker.tick",
        "Core Machines",
        "Create's generic block entity tick dispatcher used by many machine blocks.",
        "Broad machine lag. High values usually mean many Create machines are ticking, or one machine family is overworked."
    ),
    FLUID_TRANSPORT_TICK(
        "Fluid Network Tick",
        "FluidTransportBehaviour.tick",
        "Fluids",
        "Fluid transfer logic for Create pipes and related fluid movement behaviours.",
        "Fluid systems feel expensive: large pipe grids, busy tanks, or repeated flow updates can amplify server cost."
    ),
    PIPE_CONNECTION_MANAGE_FLOWS(
        "Pipe Flow Resolution",
        "PipeConnection.manageFlows",
        "Fluids",
        "Evaluates how fluid should move through a pipe connection.",
        "Often points to dense or highly active fluid pipe networks recalculating flow repeatedly."
    ),
    PIPE_CONNECTION_MANAGE_SOURCE(
        "Pipe Source Resolution",
        "PipeConnection.manageSource",
        "Fluids",
        "Finds or refreshes the source side of a pipe connection.",
        "Can indicate source-search churn in complicated fluid layouts."
    ),
    FUNNEL_TICK(
        "Funnel Tick",
        "FunnelBlockEntity.tick",
        "Funnels",
        "Server tick for Create funnels handling item insertion and extraction state.",
        "Item routing or extraction lines may be busy, blocked, or multiplied across many funnels."
    ),
    INV_MANIPULATION_EXTRACT(
        "Inventory Extraction",
        "InvManipulationBehaviour.extract",
        "Funnels",
        "Low-level Create extraction routine used by funnels and related logistics blocks.",
        "Usually means inventories are being queried heavily, often in item transport setups."
    ),
    BELT_SEGMENT_TICK(
        "Belt Segment Tick",
        "BeltBlockEntity.tick",
        "Belts",
        "Tick cost for non-controller belt segments, primarily Create kinetic-network and block-entity maintenance.",
        "High call counts with a low average cost usually indicate many loaded belt segments rather than busy transported items."
    ),
    BELT_CONTROLLER_TICK(
        "Belt Controller Tick",
        "BeltBlockEntity.tick [controller]",
        "Belts",
        "Controller belt tick including transported stack movement, belt-side interactions, and entity passengers.",
        "High average cost points to busy transported stacks, processing, funnels, tunnels, crushers, or entity passengers."
    ),
    BELT_INVENTORY_EMPTY_TICK(
        "Empty Belt Inventory Tick",
        "BeltInventory.tick [empty]",
        "Belts",
        "Controller inventory maintenance when no transported item stacks are present.",
        "A high total with many calls indicates large numbers of loaded belt controllers even when item transport is idle."
    ),
    BELT_INVENTORY_ACTIVE_TICK(
        "Active Belt Inventory Tick",
        "BeltInventory.tick [active]",
        "Belts",
        "Movement and interaction work for a controller carrying one or more transported item stacks.",
        "High values indicate item-dense belt lines or repeated funnel, tunnel, crusher, and processing checks."
    ),
    BELT_FUNNEL_CHECK(
        "Belt Funnel Check",
        "BeltFunnelInteractionHandler.checkForFunnels",
        "Belts",
        "Checks whether transported belt items should interact with nearby funnels.",
        "High values usually mean funnel-heavy belt lines are repeatedly testing insertion and extraction conditions."
    ),
    DEPLOYER_INSERT(
        "Deployer Item Insert",
        "DeployerItemHandler.insertItem",
        "Deployers",
        "Handles item insertion attempts into deployer hands and overflow slots.",
        "Common in belt-fed deployer arrays, especially when deployer hands are full or blocked."
    ),
    DIVING_BOOTS(
        "Diving Boots Equipment Check",
        "DivingBootsItem.affects",
        "Equipment",
        "Checks whether Diving Boots should affect a living entity and updates its marker tag.",
        "Not usually a full lag source by itself, but can become noisy when many entities repeatedly hit the check."
    ),
    SPOUT_ON_ITEM_RECEIVED(
        "Spout Belt Intake",
        "SpoutBlockEntity.onItemReceived",
        "Spouts",
        "Runs when a spout receives an item from belt processing.",
        "Common hotspot in fast filling lines where the same item states are checked over and over."
    ),
    SPOUT_WHEN_ITEM_HELD(
        "Spout Held Item Tick",
        "SpoutBlockEntity.whenItemHeld",
        "Spouts",
        "Runs while a spout is actively holding and processing an item.",
        "Frequent in belt plus spout chains, especially when many items wait under spouts."
    ),
    FILLING_CAN_ITEM_BE_FILLED(
        "Spout Fill Check",
        "FillingBySpout.canItemBeFilled",
        "Spouts",
        "Recipe lookup gate that decides whether an item can be filled by a spout.",
        "High values usually mean repeated recipe checks for similar item states."
    ),
    FILLING_GET_REQUIRED_AMOUNT(
        "Spout Amount Lookup",
        "FillingBySpout.getRequiredAmountForItem",
        "Spouts",
        "Recipe lookup that determines how much fluid is required for a fill operation.",
        "Often expensive in repeated belt filling chains with many identical checks."
    ),
    PACKAGE_ENTITY_TICK(
        "Package Entity Tick",
        "PackageEntity.tick",
        "Packages",
        "Server tick for Create's package entity used by package logistics.",
        "Too many active packages can trigger cross-mod entity hooks and create logistics-related lag spikes."
    ),
    GLOBAL_LOGISTICS_TICK(
        "Global Logistics Tick",
        "GlobalLogisticsManager.tick",
        "Logistics",
        "Create's global logistics manager tick handling network-wide package and routing updates.",
        "Can point to heavy package/logistics activity or oversized factory routing graphs."
    ),
    GLOBAL_RAILWAY_TICK(
        "Global Railway Tick",
        "GlobalRailwayManager.tick",
        "Trains",
        "Central train and railway state tick for Create's rail systems.",
        "Useful when train-heavy servers see tick cost rise during routeing or rail updates."
    ),
    REDSTONE_LINK_UPDATE(
        "Redstone Link Update",
        "RedstoneLinkNetworkHandler.updateNetworkOf",
        "Redstone",
        "Updates Create Redstone Link network state for a given key/channel.",
        "Repeated changes or dense wireless redstone usage can make this more visible."
    ),
    CHAIN_CONVEYOR_TICK(
        "Chain Conveyor Tick",
        "ServerChainConveyorHandler.tick",
        "Logistics",
        "Server tick for Create chain conveyor handling.",
        "Can reveal expensive chain conveyor sections or overactive item handoff paths."
    ),
    TRAIN_MAP_SYNC_TICK(
        "Train Map Sync",
        "TrainMapSync.serverTick",
        "Trains",
        "Server-side synchronization of Create train map data.",
        "Mostly a sync/visibility cost; spikes may appear when train state changes often."
    ),
    TRACK_GRAPH_SYNC_TICK(
        "Track Graph Sync",
        "TrackGraphSync.serverTick",
        "Trains",
        "Server-side synchronization of Create track graph data.",
        "Can become relevant in large rail networks or after frequent track graph changes."
    );

    private final String label;
    private final String displayName;
    private final String family;
    private final String description;
    private final String symptoms;

    ProfiledSection(String label, String displayName, String family, String description, String symptoms) {
        this.label = label;
        this.displayName = displayName;
        this.family = family;
        this.description = description;
        this.symptoms = symptoms;
    }

    public String label() {
        return label;
    }

    public String displayName() {
        return displayName;
    }

    public String family() {
        return family;
    }

    public String description() {
        return description;
    }

    public String symptoms() {
        return symptoms;
    }
}
