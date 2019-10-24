package edu.mcw.rgd.pipelines;

import edu.mcw.rgd.dao.AbstractDAO;

/**
 * Created by mtutaj on 5/30/2017.
 * <p>
 * All database code lands here
 */
public class Dao {

    AbstractDAO adao = new AbstractDAO();

    public int deleteManuallyCreatedQtlRsoAnnotations(int createdBy) throws Exception {
        String sql = "delete from FULL_ANNOT fa\n" +
                "WHERE fa.rgd_object_key=6 AND fa.aspect='S' AND fa.last_modified_by <> ?";
        return adao.update(sql, createdBy);
    }

    public int markAnnotationsForProcessing(int createdBy) throws Exception {
        String sql = "UPDATE FULL_ANNOT\n" +
                "SET FULL_ANNOT.LAST_MODIFIED_DATE=to_date('01/01/1900', 'MM/DD/YYYY')\n" +
                "WHERE FULL_ANNOT.LAST_MODIFIED_BY=?";
        return adao.update(sql, createdBy);
    }

    public int updateQtlRsoAnnotations(int createdBy) throws Exception {
        String sql = "UPDATE FULL_ANNOT fa\n" +
                "SET\n" +
                "  (\n" +
                "    term,\n" +
                "    object_symbol,\n" +
                "    object_name,\n" +
                "    last_modified_date\n" +
                "  )\n" +
                "  =\n" +
                "  (SELECT ot.TERM,\n" +
                "    qtls.QTL_SYMBOL,\n" +
                "    qtls.QTL_NAME,\n" +
                "    sysdate\n" +
                "  FROM ONT_TERMS ot,\n" +
                "    qtls,\n" +
                "    rgd_ids\n" +
                "  WHERE \n" +
                "   fa.TERM_ACC              = ot.TERM_ACC\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = qtls.RGD_ID\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = rgd_ids.RGD_ID\n" +
                "  AND rgd_ids.OBJECT_STATUS      = 'ACTIVE'\n" +
                "  )\n" +
                "WHERE fa.LAST_MODIFIED_BY=?\n" +
                "AND EXISTS\n" +
                "  (SELECT ot.TERM,\n" +
                "    qtls.QTL_SYMBOL,\n" +
                "    qtls.QTL_NAME,\n" +
                "    sysdate\n" +
                "  FROM ONT_TERMS ot,\n" +
                "    qtls,\n" +
                "    rgd_ids\n" +
                "  WHERE fa.TERM_ACC              = ot.TERM_ACC\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = qtls.RGD_ID\n" +
                "  AND fa.ANNOTATED_OBJECT_RGD_ID = rgd_ids.RGD_ID\n" +
                "  AND rgd_ids.OBJECT_STATUS      = 'ACTIVE'\n" +
                ")";
        return adao.update(sql, createdBy);
    }

    public int deleteQtlRsoAnnotations(int createdBy) throws Exception {
        String sql = "DELETE FROM full_annot fa\n" +
                "WHERE fa.last_modified_by=? " +
                "AND fa.LAST_MODIFIED_DATE = to_date('01/01/1900', 'MM/DD/YYYY')";
        return adao.update(sql, createdBy);
    }

    public int insertQtlRsoAnnotations(int createdBy) throws Exception {
        String sql = "INSERT\n" +
                "INTO FULL_ANNOT\n" +
                "  (\n" +
                "    FULL_ANNOT_KEY,\n" +
                "    term,\n" +
                "    annotated_object_rgd_id,\n" +
                "    rgd_object_key,\n" +
                "    data_src,\n" +
                "    object_symbol,\n" +
                "    ref_rgd_id,\n" +
                "    evidence,\n" +
                "    aspect,\n" +
                "    object_name,\n" +
                "    created_date,\n" +
                "    last_modified_date,\n" +
                "    term_acc,\n" +
                "    created_by,\n" +
                "    last_modified_by\n" +
                "  )\n" +
                "SELECT FULL_ANNOT_SEQ.nextval,\n" +
                "  a.*\n" +
                "FROM\n" +
                "  ( SELECT DISTINCT ot.TERM AS term,\n" +
                "    qtls.rgd_id             AS rgd_id,\n" +
                "    6                       AS rgd_object_key,\n" +
                "    'RGD'                   AS data_src,\n" +
                "    qtls.qtl_symbol         AS symbol,\n" +
                "    fa1.ref_rgd_id          AS ref,\n" +
                "    'IEA'                   AS evidence,\n" +
                "    'S'                     AS aspect,\n" +
                "    qtls.qtl_name           AS name,\n" +
                "    sysdate                 AS created_date,\n" +
                "    sysdate                 AS last_modified_date,\n" +
                "    ot.term_acc             AS term_acc,\n" +
                "    ?                       AS created_by,\n" +
                "    ?                       AS last_modified_by\n" +
                "  FROM ONT_TERMS ot,\n" +
                "    ONT_SYNONYMS os,\n" +
                "    strains st,\n" +
                "    rgd_qtl_strain rqs,\n" +
                "    qtls,\n" +
                "    full_annot fa1,\n" +
                "    rgd_ids\n" +
                "  WHERE st.strain_key = rqs.strain_key\n" +
                "  AND rqs.qtl_key     = qtls.qtl_key\n" +
                "  AND os.SYNONYM_NAME LIKE 'RGD ID:%'\n" +
                "  AND to_number(SUBSTR(os.SYNONYM_NAME,9, 100)) = st.RGD_ID\n" +
                "  AND ot.TERM_ACC                               = os.TERM_ACC\n" +
                "  AND qtls.rgd_id                               = rgd_ids.rgd_id\n" +
                "  AND rgd_ids.OBJECT_STATUS                     = 'ACTIVE'\n" +
                "  AND qtls.rgd_id                               = fa1.annotated_object_rgd_id\n" +
                "  AND fa1.aspect                                = 'L'\n" +
                "  AND NOT EXISTS\n" +
                "    (SELECT fa.FULL_ANNOT_KEY\n" +
                "    FROM FULL_ANNOT fa\n" +
                "    WHERE fa.TERM_ACC              = ot.TERM_ACC\n" +
                "    AND fa.ANNOTATED_OBJECT_RGD_ID = qtls.rgd_id\n" +
                "    AND fa.ref_rgd_id = fa1.ref_rgd_id\n" +
                "    AND fa.last_modified_by = ?\n" +
                "    )\n" +
                "  ) a";
        return adao.update(sql, createdBy, createdBy, createdBy);
    }
}
