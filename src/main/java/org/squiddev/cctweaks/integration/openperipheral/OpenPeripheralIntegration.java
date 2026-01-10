package org.squiddev.cctweaks.integration.openperipheral;

//import org.squiddev.cctweaks.core.utils.DebugLogger;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.integration.ModIntegration;

import cpw.mods.fml.common.Optional;
import openperipheral.api.ApiAccess;
import openperipheral.api.adapter.IPeripheralAdapterRegistry;

/**
 * OpenPeripheral integration
 */
public class OpenPeripheralIntegration extends ModIntegration {

    public OpenPeripheralIntegration() {
        super("OpenPeripheral");
    }

    @Override
    @Optional.Method(modid = "OpenPeripheral")
    public void postInit() {
        try {
            if (ApiAccess.isApiPresent(IPeripheralAdapterRegistry.class)) {
                if (CCTweaks.openPeripheralInventories) {
                    //DebugLogger.debug("Registering AdapterNetworkedInventory");
                    ApiAccess.getApi(IPeripheralAdapterRegistry.class)
                        .register(new AdapterNetworkedInventory());
                }
            } else {
                //DebugLogger.error("Cannot register with OpenPeripheral: IPeripheralAdapterRegistry is not present");
            }
        } catch (IllegalStateException e) {
            //DebugLogger.error("Cannot register with OpenPeripheral", e);
        }
    }
}
