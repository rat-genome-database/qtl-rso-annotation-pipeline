package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by mtutaj on 5/30/2017.
 * <p>
 * All database code lands here
 */
public class Dao {

    AnnotationDAO adao = new AnnotationDAO();
    Logger logInsertAnnots = LogManager.getLogger("insertedAnnots");
    Logger logDeleteAnnots = LogManager.getLogger("deletedAnnots");

    public List<Annotation> getQtlRsoAnnotationsNotCreatedByPipeline(int createdBy) throws Exception {
        String sql = """
            SELECT * FROM full_annot fa
            WHERE fa.rgd_object_key=6 AND fa.aspect='S' AND fa.created_by<>?
            """;
        return adao.executeAnnotationQuery(sql, createdBy);
    }

    public List<Annotation> getInRgdAnnotations(int createdBy) throws Exception {
        Date cutoffDate = Utils.addDaysToDate(new Date(), 1); // 1 day forward
        String aspect = "S";
        return adao.getAnnotationsModifiedBeforeTimestamp(createdBy, cutoffDate, aspect);
    }

    public List<Annotation> getIncomingAnnotations(int createdBy) throws Exception {

        String sql = """
            SELECT DISTINCT ot.TERM AS term,
                   qtls.rgd_id      AS rgd_id,
                   qtls.qtl_symbol  AS symbol,
                   fa1.ref_rgd_id   AS ref,
                   qtls.qtl_name    AS name,
                   ot.term_acc      AS term_acc
            FROM ONT_TERMS ot,
                ONT_SYNONYMS os,
                strains st,
                rgd_qtl_strain rqs,
                qtls,
                full_annot fa1,
                rgd_ids
            WHERE st.strain_key = rqs.strain_key
              AND rqs.qtl_key     = qtls.qtl_key
              AND os.SYNONYM_NAME LIKE 'RGD ID:%'
              AND to_number(SUBSTR(os.SYNONYM_NAME,9, 100)) = st.RGD_ID
              AND ot.TERM_ACC                               = os.TERM_ACC
              AND qtls.rgd_id                               = rgd_ids.rgd_id
              AND rgd_ids.OBJECT_STATUS                     = 'ACTIVE'
              AND qtls.rgd_id                               = fa1.annotated_object_rgd_id
              AND fa1.aspect                                = 'L'
            """;

        List<Annotation> annots = new ArrayList<Annotation>();

        try( Connection conn = DataSourceFactory.getInstance().getDataSource().getConnection() ) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while( rs.next() ) {
                String termName = rs.getString(1);
                int qtlRgdId = rs.getInt(2);
                String qtlSymbol = rs.getString(3);
                int refRgdId = rs.getInt(4);
                String qtlName = rs.getString(5);
                String termAcc = rs.getString(6);

                Annotation a = new Annotation();
                a.setTerm(termName);
                a.setAnnotatedObjectRgdId(qtlRgdId);
                a.setRgdObjectKey(6);
                a.setDataSrc("RGD");
                a.setObjectSymbol(qtlSymbol);
                a.setRefRgdId(refRgdId);
                a.setEvidence("IEA");
                a.setAspect("S");
                a.setObjectName(qtlName);
                a.setTermAcc(termAcc);
                a.setCreatedBy(createdBy);
                a.setLastModifiedBy(createdBy);

                annots.add(a);
            }
        }
        return annots;
    }

    public void insertAnnot(Annotation annot) throws Exception {

        adao.insertAnnotation(annot);
        logInsertAnnots.debug(annot.dump("|"));
    }

    public void deleteAnnot(Annotation annot) throws Exception {

        adao.deleteAnnotation(annot.getKey());
        logDeleteAnnots.debug(annot.dump("|"));
    }
}
