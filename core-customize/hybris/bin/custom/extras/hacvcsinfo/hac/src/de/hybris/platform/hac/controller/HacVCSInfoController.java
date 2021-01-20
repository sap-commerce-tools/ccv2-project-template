package de.hybris.platform.hac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Controller
@RequestMapping("/hacvcsinfo/**")
public class HacVCSInfoController
{

	@Resource(name = "vcsProperties")
	private Properties vcsProperties;


	@RequestMapping(value = "/vcs", method = RequestMethod.GET)
	public String showVCS(final Model model)
	{
		Map<String, String> propertymap = new HashMap<>();
		for (String stringPropertyName : vcsProperties.stringPropertyNames()) {
			propertymap.put(stringPropertyName, vcsProperties.getProperty(stringPropertyName, ""));
		}
		model.addAttribute("items", propertymap);
		return "vcsinfo";
	}

}
