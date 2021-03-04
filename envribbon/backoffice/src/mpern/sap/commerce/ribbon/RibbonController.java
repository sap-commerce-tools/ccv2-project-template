package mpern.sap.commerce.ribbon;

import com.hybris.cockpitng.util.DefaultWidgetController;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Div;

public class RibbonController extends DefaultWidgetController {
    @Wire
    Div envRibbon;

    @WireVariable
    EnvironmentMetaDataService environmentMetaDataService;

    @Override
    public void initialize(Component comp) {
        super.initialize(comp);
        EnvironmentMetaData environmentMetaData = environmentMetaDataService.getMetaData();
        envRibbon.setClientDataAttribute("environment", environmentMetaData.getCode());
        envRibbon.setClientDataAttribute("type", environmentMetaData.getType());
        envRibbon.setClientDataAttribute("aspect", environmentMetaData.getAspect());
    }
}
