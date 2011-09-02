package net.mad.ads.server.utils.listener;



import java.io.File;

import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Timer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.PropertyConfigurator;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import net.mad.ads.base.api.importer.Importer;
import net.mad.ads.base.api.importer.reader.*;
import net.mad.ads.base.api.BaseContext;
import net.mad.ads.base.api.track.TrackingService;
import net.mad.ads.db.db.AdDB;
import net.mad.ads.db.definition.BannerDefinition;
import net.mad.ads.db.enums.BannerType;
import net.mad.ads.server.utils.AdServerConstants;
import net.mad.ads.server.utils.RuntimeContext;
import net.mad.ads.server.utils.listener.configuration.development.DevelopmentModule;
import net.mad.ads.server.utils.listener.configuration.production.ProductionModule;
import net.mad.ads.server.utils.runnable.AdDbUpdateTask;
import de.marx.common.template.TemplateManager;
import de.marx.common.template.impl.freemarker.FMTemplateManager;
import de.marx.common.tools.Strings;
import de.marx.services.geo.IPLocationDB;
import de.marx.services.geo.MaxmindIpLocationDB;



/**
 * 
 * @author thorsten
 */
public class StartupPlugIn implements ServletContextListener {

	private static final Logger logger = LoggerFactory.getLogger(StartupPlugIn.class);
	
	private Timer timer = new Timer();
	
	private Injector injector = null;
	
	public void contextInitialized(ServletContextEvent event) {
		try {
			// Konfiguration einlesen
			String enviroment = event.getServletContext().getInitParameter("enviroment");
			
			RuntimeContext.setEnviroment(enviroment);
			String path = event.getServletContext().getRealPath("/");
			RuntimeContext.setConfiguration(AdServerConstants.CONFIG.PATHES, AdServerConstants.PATHES.WEB, path);
			RuntimeContext.getProperties().load(new FileReader(path + "/WEB-INF/config_" + enviroment +  ".properties"));
			
			if (enviroment.equalsIgnoreCase("development")) {
				injector = Guice.createInjector(new DevelopmentModule());
			} else if (enviroment.equalsIgnoreCase("production")) {
				injector = Guice.createInjector(new ProductionModule());
			}
			
			// Banner-Datenbank initialisieren
			logger.info("init bannerDB");
			initBannerDB();
			// Ip-Datenbank initialisieren
			logger.info("init ipDB");
			initIpDB();
			logger.info("init trackService");
			intServices();
			
			logger.info("init banner templates");
			initBannerTemplates(path);
			
			logger.info("init templates");
			initTemplates(path + "/WEB-INF/content/templates/");
			
			timer.scheduleAtFixedRate(new AdDbUpdateTask(), AdDbUpdateTask.delay, AdDbUpdateTask.period);
			
			RuntimeContext.cacheManager = new DefaultCacheManager("resources/config/infinispan_config.xml");
			RuntimeContext.requestBanners = RuntimeContext.cacheManager.getCache("requestBanners");
			RuntimeContext.requestBanners.addListener(new CacheListener());
			
			RuntimeContext.setImporter(new Importer(RuntimeContext.getProperties().getProperty(AdServerConstants.CONFIG.PROPERTIES.BANNER_IMPORT_DIRECOTRY), RuntimeContext.getAdDB()));
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public void contextDestroyed(ServletContextEvent event) {
		try {
			RuntimeContext.getAdDB().close();
			RuntimeContext.getIpDB().close();
			RuntimeContext.getTrackService().close();
			RuntimeContext.cacheManager.stop();
			
			timer.cancel();
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	private void intServices () throws Exception {
		RuntimeContext.setTrackService(injector.getInstance(TrackingService.class));
	}
	
	private void initTemplates (String path) throws IOException {
		File tdir = new File(path);
		File[] templates = tdir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".ftl")) {
					return true;
				}
				return false;
			}
		});
		
		TemplateManager tempMan = new FMTemplateManager();
		tempMan.init(path);
		for (File template : templates) {
			String tname = Strings.removeExtension(template.getName()).toLowerCase();
			tempMan.registerTemplate(tname, template.getName());
		}
		
		RuntimeContext.setTemplateManager(tempMan);
	}
	
	private void initBannerTemplates (String path) throws IOException {
//		String templatePath = RuntimeContext.getProperties().getProperty(AdServerConstants.CONFIG.PROPERTIES.BANNER_TEMPLATE_DIR);
		String templatePath = path + "/WEB-INF/content/templates/banner";
		
		RuntimeContext.getBannerRenderer().init(templatePath);
		
		for (BannerType type : BannerType.values()) {
			RuntimeContext.getBannerRenderer().registerTemplate(type.getName().toLowerCase(), type.getName().toLowerCase()+".ftl");
		}
	}
	
	private void initIpDB () throws Exception {
		
		long before = System.currentTimeMillis();
		
		IPLocationDB db = injector.getInstance(IPLocationDB.class);
		db.open(RuntimeContext.getProperties().getProperty(AdServerConstants.CONFIG.PROPERTIES.IPDB_DIR));
		RuntimeContext.setIpDB(db);
		
		RuntimeContext.getIpDB().searchIp("213.83.37.145");
		long after = System.currentTimeMillis();
		logger.debug("finish ipDB: " + (after - before) + "ms");
	}
	
	private void initBannerDB () throws Exception {
		
		long before = System.currentTimeMillis();
		
		RuntimeContext.setAdDB(new AdDB());
		RuntimeContext.getAdDB().open();
		String bannerPath = RuntimeContext.getProperties().getProperty(AdServerConstants.CONFIG.PROPERTIES.BANNER_DATA_DIRECOTRY);
		
		File bdir = new File(bannerPath);
		if (bdir.exists() && bdir.isDirectory()) {
			String[] banners = bdir.list(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					if (name.endsWith(".xml")) {
						return true;
					}
					return false;
				}
			});
			
			for (String banner : banners) {
				BannerDefinition b = AdXmlReader.readBannerDefinition(bannerPath + File.separator + banner);
				RuntimeContext.getAdDB().addBanner(b);
			}
		}
		
		RuntimeContext.getAdDB().reopen();
		
		long after = System.currentTimeMillis();
		logger.debug("finish bannerDB: " + (after - before) + "ms");
	}
}