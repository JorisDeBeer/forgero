package com.sigmundgranaas.forgero.core.state.composite;

import com.sigmundgranaas.forgero.core.soul.Soul;
import com.sigmundgranaas.forgero.core.soul.SoulContainer;
import com.sigmundgranaas.forgero.core.state.IdentifiableContainer;
import com.sigmundgranaas.forgero.core.state.State;
import com.sigmundgranaas.forgero.core.state.upgrade.slot.SlotContainer;

public class SoulBoundTool extends ConstructedTool implements SoulContainer {
    private final Soul soul;

    public SoulBoundTool(State head, State handle, State baseMaterial, SlotContainer slots, IdentifiableContainer id, Soul soul) {
        super(head, handle, baseMaterial, slots, id);
        this.soul = soul;
    }

    @Override
    public Soul getSoul() {
        return soul;
    }

    @Override
    public SoulBoundToolBuilder toolBuilder() {
        return SoulBoundToolBuilder.of(super.toolBuilder(), soul);
    }

    public static class SoulBoundToolBuilder extends ToolBuilder {
        private Soul soul;

        public SoulBoundToolBuilder(State head, State handle, State material, Soul soul) {
            super(head, handle, material);
            this.soul = soul;
        }

        public static SoulBoundToolBuilder builder(State head, State handle, State material, Soul soul) {
            return new SoulBoundToolBuilder(head, handle, material, soul);
        }

        public static SoulBoundToolBuilder of(ToolBuilder builder, Soul soul) {
            var soulBuilder = new SoulBoundToolBuilder(builder.getHead(), builder.getHandle(), builder.getPrimaryMaterial(), soul);
            soulBuilder.type(builder.getType());
            soulBuilder.name(builder.getName());
            soulBuilder.nameSpace(builder.getNameSpace());
            soulBuilder.addSlotContainer(builder.getUpgradeContainer());
            return soulBuilder;
        }

        public SoulBoundToolBuilder soul(Soul soul) {
            this.soul = soul;
            return this;
        }

        public SoulBoundTool build() {
            compositeName();
            var id = new IdentifiableContainer(name, nameSpace, type);
            return new SoulBoundTool(head, handle, primaryMaterial, upgradeContainer, id, soul);
        }
    }
}
