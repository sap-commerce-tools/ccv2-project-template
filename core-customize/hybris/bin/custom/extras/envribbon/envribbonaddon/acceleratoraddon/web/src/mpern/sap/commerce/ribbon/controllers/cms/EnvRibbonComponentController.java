package mpern.sap.commerce.ribbon.controllers.cms;

import de.hybris.platform.addonsupport.controllers.cms.AbstractCMSAddOnComponentController;
import mpern.sap.commerce.ribbon.EnvironmentMetaDataService;
import mpern.sap.commerce.ribbon.model.EnvRibbonComponentModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller("EnvRibbonComponentController")
@RequestMapping("/view/" + EnvRibbonComponentModel._TYPECODE + "Controller")
public class EnvRibbonComponentController extends AbstractCMSAddOnComponentController<EnvRibbonComponentModel> {

    @Resource
    private EnvironmentMetaDataService environmentMetaDataService;

    @Override
    protected void fillModel(HttpServletRequest request, Model model, EnvRibbonComponentModel component) {
        model.addAttribute("env", environmentMetaDataService.getMetaData());
    }
}
