package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.util.*;

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

        // if there are any QTL-RSO annotations not created by the pipeline, abort
        List<Annotation> annots = dao.getQtlRsoAnnotationsNotCreatedByPipeline(getCreatedBy());
        if( !annots.isEmpty() ) {
            log.error("ABORTING PIPELINE: found QTL-RSO annotations that were not created by the pipeline");
            log.error("    (previous logic was to delete these annotations)");
            for( Annotation a: annots ) {
                log.warn("   "+a.dump("|"));
            }
            throw new Exception("ABORT due to presence of non-pipeline QTL-RSO annotations");
        }

        List<Annotation> incomingAnnots = dao.getIncomingAnnotations(getCreatedBy());

        List<Annotation> inRgdAnnots = dao.getInRgdAnnotations(getCreatedBy());

        qcAnnots(incomingAnnots, inRgdAnnots, dao);

        String msg = "=== OK === elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis());
        log.info(msg);
    }

    void qcAnnots( List<Annotation> incomingAnnots, List<Annotation> inRgdAnnots, Dao dao ) throws Exception {

        Map<String,Annotation> incomingMap = new HashMap<>();
        for( Annotation a: incomingAnnots ) {
            String key = createAnnotKey(a);
            Annotation prevAnnot = incomingMap.put(key, a);
            if( prevAnnot!=null ) {
                System.out.println("incoming annot duplicate");
            }
        }
        log.info("incoming annot count: "+incomingMap.size());

        Map<String,Annotation> inRgdMap = new HashMap<>();
        for( Annotation a: inRgdAnnots ) {
            String key = createAnnotKey(a);
            Annotation prevAnnot = inRgdMap.put(key, a);
            if( prevAnnot!=null ) {
                System.out.println("in-rgd annot duplicate");
            }
        }
        log.info("in-rgd annot count: "+inRgdMap.size());

        Set<String> matchingAnnots = new HashSet<>(incomingMap.keySet());
        matchingAnnots.retainAll(inRgdMap.keySet());
        log.info("matching annot count: "+matchingAnnots.size());

        Set<String> forInsertAnnots = new HashSet<>(incomingMap.keySet());
        forInsertAnnots.removeAll(inRgdMap.keySet());
        if( !forInsertAnnots.isEmpty() ) {
            for( String key: forInsertAnnots ) {
                Annotation a = incomingMap.get(key);
                dao.insertAnnot(a);
            }
            log.info("inserted annot count: " + forInsertAnnots.size());
        }

        Set<String> forDeleteAnnots = new HashSet<>(inRgdMap.keySet());
        forDeleteAnnots.removeAll(incomingMap.keySet());
        if( !forDeleteAnnots.isEmpty() ) {
            for (String key : forDeleteAnnots) {
                Annotation a = inRgdMap.get(key);
                dao.deleteAnnot(a);
            }
            log.info("deleted annot count: " + forDeleteAnnots.size());
        }
    }

    private String createAnnotKey(Annotation a) {
        return a.getRefRgdId()+"|"+a.getAnnotatedObjectRgdId()+"|"+a.getTermAcc()
                +"|" + Utils.defaultString(a.getXrefSource())
                +"|" + Utils.defaultString(a.getQualifier())
                +"|" + Utils.defaultString(a.getWithInfo())
                +"|" + Utils.defaultString(a.getEvidence());
    }

    /*
    public void runOld() throws Exception {

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
*/
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
