package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.MemoryMonitor;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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

    Logger log = LogManager.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        QtlRsoAnnotation manager = (QtlRsoAnnotation) (bf.getBean("manager"));

        try {
            manager.run();
        } catch(Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run() throws Exception {

        long time0 = System.currentTimeMillis();

        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();

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

        List<Annotation> inRgdAnnots = dao.getInRgdAnnotations(getCreatedBy(), new Date(time0));

        qcAnnots(incomingAnnots, inRgdAnnots, dao);

        memoryMonitor.stop();
        log.info(memoryMonitor.getSummary());

        String msg = "=== OK === elapsed "+ Utils.formatElapsedTime(time0, System.currentTimeMillis());
        log.info(msg);
        log.info("");
    }

    void qcAnnots( List<Annotation> incomingAnnots, List<Annotation> inRgdAnnots, Dao dao ) throws Exception {

        Map<String,Annotation> incomingMap = new HashMap<>();
        for( Annotation a: incomingAnnots ) {
            String key = createAnnotKey(a);
            Annotation prevAnnot = incomingMap.put(key, a);
            if( prevAnnot!=null ) {
                log.warn("incoming annot duplicate: "+a.dump("|"));
            }
        }
        log.info("incoming annot count: "+Utils.formatThousands(incomingMap.size()));

        Map<String,Annotation> inRgdMap = new HashMap<>();
        for( Annotation a: inRgdAnnots ) {
            String key = createAnnotKey(a);
            Annotation prevAnnot = inRgdMap.put(key, a);
            if( prevAnnot!=null ) {
                log.warn("in-rgd annot duplicate: "+a.dump("|"));
            }
        }
        log.info("in-rgd annot count: "+Utils.formatThousands(inRgdMap.size()));

        Set<String> matchingAnnots = new HashSet<>(incomingMap.keySet());
        matchingAnnots.retainAll(inRgdMap.keySet());
        log.info("matching annot count: "+Utils.formatThousands(matchingAnnots.size()));

        Set<String> forInsertAnnots = new HashSet<>(incomingMap.keySet());
        forInsertAnnots.removeAll(inRgdMap.keySet());
        if( !forInsertAnnots.isEmpty() ) {
            for( String key: forInsertAnnots ) {
                Annotation a = incomingMap.get(key);
                dao.insertAnnot(a);
            }
            log.info("inserted annot count: " + Utils.formatThousands(forInsertAnnots.size()));
        }

        Set<String> forDeleteAnnots = new HashSet<>(inRgdMap.keySet());
        forDeleteAnnots.removeAll(incomingMap.keySet());
        if( !forDeleteAnnots.isEmpty() ) {
            for (String key : forDeleteAnnots) {
                Annotation a = inRgdMap.get(key);
                dao.deleteAnnot(a);
            }
            log.info("deleted annot count: " + Utils.formatThousands(forDeleteAnnots.size()));
        }
    }

    private String createAnnotKey(Annotation a) {
        return a.getRefRgdId()+"|"+a.getAnnotatedObjectRgdId()+"|"+a.getTermAcc()
                +"|" + Utils.defaultString(a.getXrefSource())
                +"|" + Utils.defaultString(a.getQualifier())
                +"|" + Utils.defaultString(a.getWithInfo())
                +"|" + Utils.defaultString(a.getEvidence())
                +"|" + Utils.defaultString(a.getQualifier2())
                +"|" + Utils.defaultString(a.getAssociatedWith());
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
