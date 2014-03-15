package org.jenkinsci.test.acceptance.resolver;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.trilead.ssh2.Connection;
import org.codehaus.plexus.util.FileUtils;
import org.jenkinsci.test.acceptance.controller.Machine;
import org.jenkinsci.test.acceptance.controller.Ssh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 * @author Vivek Pandey
 */
public class JenkinsDownloader implements JenkinsResolver {

    private final String jenkinsWarLocation;

    @Inject
    @Named("jenkins_md5_sum")
    private String jenkinsMd5Sum;

    @Inject
    public JenkinsDownloader(@Named("jenkins-war-location")String jenkinsWarLocation) {
        this.jenkinsWarLocation = jenkinsWarLocation;
    }

    @Override
    public void materialize(Machine machine, String path) {
        Ssh ssh = machine.connect();
        if(!remoteFileExists(ssh.getConnection(),path,jenkinsMd5Sum)){
            ssh.executeRemoteCommand("mkdir -p "+ FileUtils.dirname(path));
            ssh.executeRemoteCommand(String.format("wget -q -O %s %s",path, jenkinsWarLocation));
        }
    }

    public static  boolean remoteFileExists(Connection connection, String target, String expectedMd5Sum){
        try {
            int status  = connection.exec(String.format("stat %s > /dev/null 2>&1", target), System.out);
            if(status == 0){
                return expectedMd5Sum == null || validateMd5Sum(connection, target, expectedMd5Sum);
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    public static boolean validateMd5Sum(Connection connection, String target, String expectedMd5Sum){
        //jenkins.war exists, lets check the md5 sum
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int status  = 0;
        try {
            status = connection.exec(String.format("md5sum %s",target), baos);
            if(status == 0){
                String[] values = baos.toString("UTF-8").split(" ");
                return (values.length > 1 && values[0].equals(expectedMd5Sum));
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    private static final Logger logger = LoggerFactory.getLogger(JenkinsDownloader.class);
}