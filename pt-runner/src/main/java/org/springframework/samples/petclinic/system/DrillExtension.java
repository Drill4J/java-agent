package org.springframework.samples.petclinic.system;


import org.springframework.stereotype.Component;

/**
 * this object uses only for extend coverage compatibility (method diff, etc)
 */
@SuppressWarnings("unused")
@Component
class DrillExtension {
    public DrillExtension() {
        System.out.println("init");
    }
}
