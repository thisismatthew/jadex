package jadex.android.standalone.platformapp;

import jadex.android.commons.Logger;
import jadex.android.platformapp.R;
import jadex.android.service.JadexPlatformManager;
import jadex.android.standalone.JadexApplication;
import jadex.android.standalone.clientapp.ClientAppFragment;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import dalvik.system.DexClassLoader;

public class JadexApplicationLoader extends FragmentActivity
{
	private static String defaultEntryActivityName = "jadex.android.platformapp.DefaultApplication";
	private LayoutInflater userAppInflater;
	private String userAppPackage;
	private Context userAppContext;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// set default layout inflater during onCreate()
		userAppInflater = super.getLayoutInflater();
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		FragmentManager manager = getSupportFragmentManager();
		FragmentTransaction ta = manager.beginTransaction();
		if (intent != null && JadexApplication.INTENT_ACTION_LOADAPP.equals(intent.getAction())) {
			String appPath = intent.getStringExtra(JadexApplication.EXTRA_KEY_APPLICATIONPATH);
			String className = intent.getStringExtra(JadexApplication.EXTRA_KEY_ACTIVITYCLASS);
			userAppPackage = intent.getStringExtra(JadexApplication.EXTRA_KEY_APPLICATIONPACKAGE);
			if (className == null)
			{
				className = defaultEntryActivityName;
			}
			
			if (appPath != null) {
				ClientAppFragment act = loadAndCreateUserActivity(appPath, className);
				act.onPrepare(this);
				ta.add(R.id.fragmentContainer, act);
			} else {
				Logger.e("Please specify an Activity class to start with EXTRA_KEY_ACTIVITYCLASS!");
				finish();
				return;
			}
			
		} else {
			Logger.e("Please start this application with action net.sourceforge.jadex.LOAD_APPLICATION");
			finish();
			return;
		}
		setContentView(R.layout.loaderlayout);
		// set custom layout inflater during user app lifetime
		userAppInflater = createUserAppInflater(userAppPackage);
		ta.commit();
	}
	
	private LayoutInflater createUserAppInflater(String userApplicationPackage)
	{
		try
		{
			userAppContext = getApplicationContext().createPackageContext(userApplicationPackage, Context.CONTEXT_IGNORE_SECURITY);
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		
		LayoutInflater inflater = LayoutInflater.from(userAppContext);
		return inflater;
	}
	
	@Override
	public Context getApplicationContext()
	{
		if (userAppContext == null) {
			return super.getApplicationContext();
		} else {
			System.out.println("Custom context returned");
			return userAppContext;
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}
	
	@Override
	public LayoutInflater getLayoutInflater()
	{
		return userAppInflater;
	}

	private ClientAppFragment loadAndCreateUserActivity(String appPath, String className)
	{
		DexClassLoader cl = getClassLoaderForExternalDex(getClassLoader(), appPath);
		JadexPlatformManager.getInstance().setAppClassLoader(appPath, cl);
		try
		{
			Class<ClientAppFragment> actClass = (Class<ClientAppFragment>) cl.loadClass(className);
//			Constructor<ClientAppFragment> actCon = actClass.getConstructor();
			ClientAppFragment act = actClass.newInstance();
			return act;
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (SecurityException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (InstantiationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private DexClassLoader getClassLoaderForExternalDex(ClassLoader parent, String appPath)
	{
//		File dexInternalStoragePath = new File(getDir("dex", Context.MODE_PRIVATE), "jadex.jar");
		final File optimizedDexOutputPath = getDir("outdex", Context.MODE_PRIVATE);

		DexClassLoader cl = new DexClassLoader(appPath, optimizedDexOutputPath.getAbsolutePath(), null,
				parent);

		return cl;
	}
	
	
	private ClassLoader getClassLoaderForInternalClasses(ClassLoader parent)
	{
		PackageManager pm = getPackageManager();

		ApplicationInfo applicationInfo = this.getApplicationInfo();
		
		final File optimizedDexOutputPath = getDir("outdex", Context.MODE_PRIVATE);
		
		return new DexClassLoader(applicationInfo.sourceDir, optimizedDexOutputPath.getAbsolutePath(), null, parent);
		
//		return new ParentLastPathClassLoader(applicationInfo.sourceDir, parent);
		
//		for (ApplicationInfo app : pm.getInstalledApplications(0)) {
//			if (applicationInfo.packageName.equals(app.packageName)) {
//				
//			}
//		  Log.d("PackageList", "package: " + app.packageName + ", sourceDir: " + app.sourceDir);
//		}
	}

}
