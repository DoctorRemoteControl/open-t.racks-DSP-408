package de.drremote.dsp408controller.runtime;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "DSP408 Controller",
        description = "Karaf/OSGi configuration for the DSP408 Controller"
)
public @interface Dsp408Configuration {

    @AttributeDefinition(name = "DSP ID")
    String dsp_id() default "main";

    @AttributeDefinition(name = "DSP IP")
    String dsp_ip() default "192.168.0.166";

    @AttributeDefinition(name = "DSP Port")
    int dsp_port() default 9761;

    @AttributeDefinition(name = "Auto Connect")
    boolean auto_connect() default false;

    @AttributeDefinition(name = "Auto Read On Connect")
    boolean auto_read_on_connect() default true;

    @AttributeDefinition(name = "Volume Step dB")
    double volume_step_db() default 1.0;

    @AttributeDefinition(
            name = "Additional DSPs",
            description = "Additional DSP definitions as id=ip or id=ip:port, for example fir=192.168.0.166:9761"
    )
    String[] dsps() default {};
}
