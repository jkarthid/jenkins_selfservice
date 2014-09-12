package com.rightscale.selfservice;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalPluginConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URI;
//Jersey
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.filter.LoggingFilter;
//javax
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.UriBuilder;
//std libs
import java.util.List;


/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link Main} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class Main extends Builder {

    private final String name;
    public static Client client;
    public static URI uri;
    public static List<NewCookie> cookies;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public Main(String name) {
        this.name = name;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
   * @return 
     */
    public String getName() {
        return name;
    }
    
    public static URI build_uri(String ssUser,String ssPassword, String ssShard, String ssAccount){
      UriBuilder builder = UriBuilder.fromPath("https://my.rightscale.com");
      builder.path("/api/session");
      builder.queryParam("email", ssUser);
      builder.queryParam("password", ssPassword);
      builder.queryParam("account_href", ssAccount);
      //WebTarget webTarget = client.target("https://my.rightscale.com/api/session");
      uri = builder.build();
      return uri;
    }
    public static void login(){
      ClientConfig config = new ClientConfig();
      client = ClientBuilder.newClient(config);
      
    }
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
        /**
         * if (getDescriptor().getUseFrench())
         *  listener.getLogger().println("Bonjour, "+name+"!");
         * else
         * listener.getLogger().println("Hello, "+name+"!");
         * */
        String ssUser = getDescriptor().getSelfServiceUser();
        String ssPassword = getDescriptor().getSelfServicePassword();
        String ssShard = getDescriptor().getSelfServiceShard();
        String ssAccount = getDescriptor().getSelfServiceAccount();
        listener.getLogger().println("User: " + ssUser);
        listener.getLogger().println("Password: " + ssPassword);
        listener.getLogger().println("Shard: " + ssShard);
        listener.getLogger().println("Account: " + ssAccount);
        listener.getLogger().println("URI: " + build_uri(ssUser,ssPassword,ssShard,ssAccount).toString());
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link Main}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/Main/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean debugMode;
        private String selfserviceUser;
        private String selfservicePassword;
        private String selfserviceShard;
        private String selfserviceAccount;

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
       * @throws java.io.IOException
       * @throws javax.servlet.ServletException
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }
        /**
        public FormValidation doTestConnection(@QueryParameter String selfserviceUser,
                                               @QueryParameter String selfservicePassword,
                                               @QueryParameter String selfserviceShard,
                                               @QueryParameter String selfserviceAccount
        ) throws IOException, ServletException {
          if (selfserviceUser.length() == 0)
             return FormValidation.error("Self Service User is blank");
          return FormValidation.ok();                     
        }
        * */
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
       * @return 
         */
        @Override
        public String getDisplayName() {
            return "Launch Self Service App";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            selfserviceUser=formData.getString("selfserviceUser");
            selfservicePassword=formData.getString("selfservicePassword");
            selfserviceShard=formData.getString("selfserviceShard");
            selfserviceAccount=formData.getString("selfserviceAccount");
            debugMode=formData.getBoolean("debugMode");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         
         * public boolean getUseFrench() {
         *    return useFrench;
         *  }
         * 
         **/
        
        public String getSelfServiceUser(){
            return selfserviceUser;
        }
        public String getSelfServicePassword(){
            return selfservicePassword;
        }
        public String getSelfServiceShard(){
            return selfserviceShard;
        }
        public String getSelfServiceAccount(){
            return selfserviceAccount;
        }
        public boolean getDebugMode(){
            return debugMode;
        }
    }
}

