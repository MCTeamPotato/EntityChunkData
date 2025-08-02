package me.kall.entitychunkdata;

import me.kall.entitychunkdata.data.EntitiesInChunkData;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(EntityChunkData.MOD_ID)
public final class EntityChunkData {
    public static final String MOD_ID = "entitychunkdata";
    public static final String MOD_NAME = "EntityChunkData";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public EntityChunkData() {
        EntitiesInChunkData.register();
    }
}
