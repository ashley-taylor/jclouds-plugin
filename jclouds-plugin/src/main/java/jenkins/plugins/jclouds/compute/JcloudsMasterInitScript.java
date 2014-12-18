package jenkins.plugins.jclouds.compute;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.tasks.Shell;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jclouds.compute.domain.NodeMetadata;

import shaded.com.google.common.base.Predicate;

public class JcloudsMasterInitScript implements Predicate<NodeMetadata> {

	private String masterInitScript;

	public JcloudsMasterInitScript(String masterInitScript) {
		this.masterInitScript = masterInitScript;
	}

	@Override
	public boolean apply(NodeMetadata input) {
		TaskListener listener = new StreamTaskListener(System.out);
		Launcher launcher = new Launcher.LocalLauncher(listener);
		Shell shell = new Shell(masterInitScript);
		Map<String, String> envVars = new HashMap<String, String>();

		envVars.put("PUBLICIP", del(input.getPublicAddresses()));
		envVars.put("PRIVATEIP", del(input.getPrivateAddresses()));

		try{

			FilePath dir = new FilePath(new File(System.getProperty("java.io.tmpdir")));
			FilePath file = null;
			try{
				file = shell.createScriptFile(dir);
				int r = launcher.launch().cmds(shell.buildCommandLine(file)).envs(envVars).stdout(listener).start().join();
				return r == 0;
			}finally{
				if(file != null){
					file.delete();
				}
			}
		} catch (IOException e) {
			Util.displayIOException(e,listener);
			e.printStackTrace(listener.fatalError("JClouds Master boot"));
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace(listener.fatalError("JClouds Master boot"));
			return false;
		}
	}

	private String del(Collection<String> target){
		StringBuilder builder = new StringBuilder();
		for(String del: target){
			if(builder.length() > 0){
				builder.append(' ');
			}
			builder.append(del);
		}
		return builder.toString();
	}

}
