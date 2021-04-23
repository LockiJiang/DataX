package com.alibaba.datax.plugin.writer.oscarwriter;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.common.util.MessageSource;
import com.alibaba.datax.plugin.rdbms.util.DBUtil;
import com.alibaba.datax.plugin.rdbms.util.DBUtilErrorCode;
import com.alibaba.datax.plugin.rdbms.util.DataBaseType;
import com.alibaba.datax.plugin.rdbms.writer.CommonRdbmsWriter;
import com.alibaba.datax.plugin.rdbms.writer.Constant;
import com.alibaba.datax.plugin.rdbms.writer.Key;
import com.alibaba.datax.plugin.rdbms.writer.util.WriterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class OscarWriter extends Writer {
	private static final DataBaseType DATABASE_TYPE = DataBaseType.Oscar;
	private static final Logger LOG = LoggerFactory.getLogger(Job.class);

	public static class Job extends Writer.Job {
		private static MessageSource MESSAGE_SOURCE = MessageSource.loadResourceBundle(OscarWriter.class);
		private Configuration originalConfig = null;
		private CommonRdbmsWriter.Job commonRdbmsWriterJob;

		@Override
        public void preCheck() {
            this.init();
            this.commonRdbmsWriterJob.writerPreCheck(this.originalConfig, DATABASE_TYPE);
        }

        @Override
		public void init() {
			this.originalConfig = super.getPluginJobConf();

			String writeMode = this.originalConfig.getString("writeMode");
			if (null != writeMode) {
				throw DataXException.asDataXException(DBUtilErrorCode.CONF_ERROR,
						MESSAGE_SOURCE.message("oscarWriter.1", writeMode));
			}

			//获取建临时表sql
			String tempTable = this.originalConfig.getString("useTempTable", null);
			if (tempTable != null && !tempTable.trim().equals("")) {
				// 获取数据库连接信息
				String username = originalConfig.getString(Key.USERNAME);
				String password = originalConfig.getString(Key.PASSWORD);
				List<Object> conns = originalConfig.getList(Constant.CONN_MARK, Object.class);
				Configuration connConf = Configuration.from(conns.get(0).toString());
				String jdbcUrl = connConf.getString(Key.JDBC_URL);

				List<String> sqls = new ArrayList<String>();
				sqls.add(tempTable);
				Connection conn = DBUtil.getConnection(OscarWriter.DATABASE_TYPE, jdbcUrl, username, password);
				WriterUtil.executeSqls(conn, sqls, jdbcUrl, OscarWriter.DATABASE_TYPE);
				DBUtil.closeDBResources(null, conn);
				LOG.info("Before job init(), create temp table [{}]", tempTable);
			}

			this.commonRdbmsWriterJob = new CommonRdbmsWriter.Job(DATABASE_TYPE);
			this.commonRdbmsWriterJob.init(this.originalConfig);
		}

		@Override
		public void prepare() {
			this.commonRdbmsWriterJob.prepare(this.originalConfig);
		}

		@Override
		public List<Configuration> split(int mandatoryNumber) {
			return this.commonRdbmsWriterJob.split(this.originalConfig,
					mandatoryNumber);
		}

		@Override
		public void post() {
			this.commonRdbmsWriterJob.post(this.originalConfig);
		}

		@Override
		public void destroy() {
			this.commonRdbmsWriterJob.destroy(this.originalConfig);
		}

	}

	public static class Task extends Writer.Task {
		private Configuration writerSliceConfig;
		private CommonRdbmsWriter.Task commonRdbmsWriterTask;

		@Override
		public void init() {
			this.writerSliceConfig = super.getPluginJobConf();
			this.commonRdbmsWriterTask = new CommonRdbmsWriter.Task(DATABASE_TYPE);
			this.commonRdbmsWriterTask.init(this.writerSliceConfig);
		}

		@Override
		public void prepare() {
			this.commonRdbmsWriterTask.prepare(this.writerSliceConfig);
		}

		@Override
		public void startWrite(RecordReceiver recordReceiver) {
			this.commonRdbmsWriterTask.startWrite(recordReceiver,
					this.writerSliceConfig, super.getTaskPluginCollector());
		}

		@Override
		public void post() {
			this.commonRdbmsWriterTask.post(this.writerSliceConfig);
		}

		@Override
		public void destroy() {
			this.commonRdbmsWriterTask.destroy(this.writerSliceConfig);
		}

	}

}
