package com.sigmundgranaas.forgero.fabric;

import com.sigmundgranaas.forgero.fabric.ipn.IpnNextCompat;
import com.sigmundgranaas.forgero.fabric.toolstats.ToolStatTagGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ForgeroCompatInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("inventoryprofilesnext")) {
            IpnNextCompat.blackListForgero();
        }

        if (FabricLoader.getInstance().isModLoaded("toolstats")) {
            ToolStatTagGenerator.generateTags();
        }
    }
}
