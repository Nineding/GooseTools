package com.goosethings.tools.result;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CelebrationPolicyTest {
    @Test void paidActionsRequireBothExtensionAndTheirOwnEntitlement() {
        for(int selected=0;selected<=2;selected++) {
            assertEquals(0,CelebrationPolicy.authorized(selected,false,true,true));
            assertEquals(0,CelebrationPolicy.authorized(selected,true,false,false));
        }
        assertEquals(1,CelebrationPolicy.authorized(1,true,true,false));
        assertEquals(0,CelebrationPolicy.authorized(2,true,true,false));
        assertEquals(2,CelebrationPolicy.authorized(2,true,false,true));
        assertEquals(0,CelebrationPolicy.authorized(1,true,false,true));
        assertEquals(0,CelebrationPolicy.authorized(-1,true,true,true));
        assertEquals(0,CelebrationPolicy.authorized(3,true,true,true));
    }
}
