package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

/**
 * Created by mtutaj on 5/30/2017
 */
public class QtlRsoAnnotation {

    private String version;
    private int createdBy;

    Logger log = Logger.getRootLogger();

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        QtlRsoAnnotation manager = (QtlRsoAnnotation) (bf.getBean("manager"));

        try {
            manager.run();
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void run() throws Exception {
        long time0 = System.currentTimeMillis();

        log.info(getVersion());

        Dao dao = new Dao();

        // Delete manually created QTL-RSO annotations.
        int rowsAffected = dao.deleteManuallyCreatedQtlRsoAnnotations(getCreatedBy());
        log.info("Delete manually created annotations: "+rowsAffected);

        // Mark all annotations that were created by this pipeline: Last_modified=181
        rowsAffected = dao.markAnnotationsForProcessing(getCreatedBy());
        log.info("Annotations marked for processing: "+rowsAffected);

        // Update valid annotations with the latest terms, names, symbols and last_modified_date
        rowsAffected = dao.updateQtlRsoAnnotations(getCreatedBy());
        log.info("Annotations updated: "+rowsAffected);

        // Delete obsolete annotations which are not touched by the update annotations updates
        rowsAffected = dao.deleteQtlRsoAnnotations(getCreatedBy());
        log.info("Records deleted: "+rowsAffected);

        // Insert new annotations
        rowsAffected = dao.insertQtlRsoAnnotations(getCreatedBy());
        log.info("New records inserted: "+rowsAffected);


        String msg = "=== OK === elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis());
        log.info(msg);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public int getCreatedBy() {
        return createdBy;
    }
}
