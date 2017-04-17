/**
 * 
 */
package org.borademir.ora2ws.parser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import oracle.sql.ArrayDescriptor;
import oracle.sql.StructDescriptor;

import org.borademir.ora2ws.annotation.Ora2WsArgument;
import org.borademir.ora2ws.annotation.Ora2WsClass;
import org.borademir.ora2ws.annotation.Ora2WsField;
import org.borademir.ora2ws.annotation.Ora2WsFieldLength;
import org.borademir.ora2ws.annotation.Ora2WsMethod;
import org.borademir.ora2ws.annotation.Ora2WsPackage;
import org.borademir.ora2ws.annotation.Ora2WsTableOf;
import org.borademir.ora2ws.db.util.ConnectionProvider;
import org.borademir.ora2ws.model.Ora2WsListWrapper;
import org.borademir.ora2ws.model.Ora2WsArgumentType;
import org.borademir.ora2ws.model.Ora2WsBaseArray;
import org.borademir.ora2ws.model.Ora2WsBaseModel;
import org.borademir.ora2ws.model.Ora2WsMethodType;
import org.borademir.ora2ws.model.Ora2WsType;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JArray;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

/**
 * @author 			Bora.Demir
 * @date   			Oct 5, 2015 11:32:46 AM
 * 
 * @description		
 */
public class OracleClientGenerator {
	
	static final SimpleDateFormat PRETTY_DATE_FORMATTER = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	static final File ROOT_FOLDER = calculateSourceFolder();
	@SuppressWarnings("rawtypes")
	static Map<String, Class> FUNCTIONS_MAP = new HashMap<String, Class>();
	
	
	private String owner = "";
	private JCodeModel codeModel = new JCodeModel();
	private String runtimePackage;
	private String modelPackage;
	private String packageName ; 
	
	
	public OracleClientGenerator(String runtimePackage, String modelPackage, String packageName) {
		super();
		this.runtimePackage = runtimePackage;
		this.modelPackage = modelPackage;
		this.packageName = packageName;
	}

	public static void main(String[] args) {
//		String rPack  = "com.demo.oracle.alzhlthcpacommutils.runtime.";
//		String mPack  = "com.demo.oracle.alzhlthcpacommutils.model.";
//		String packageName = "ALZ_HLTH_CPA_COMM_UTILS";
//		new JFOracleParser(rPack,mPack,packageName).execute(JFOracleDB.DEV);
		
		
//		String rPack  = "com.demo.oracle.alztpacoreutils.runtime.";
//		String mPack  = "com.demo.oracle.alztpacoreutils.model.";
//		String packageName = "ALZ_TPA_CORE_UTILS";
//		new JFOracleParser(rPack,mPack,packageName).execute(JFOracleDB.DEV);
		
		String rPack  = "com.demo.oracle.alzpolicycoreutils.runtime.";
		String mPack  = "com.demo.oracle.alzpolicycoreutils.model.";
		String packageName = "ALZ_POLICY_CORE_UTILS";
		new OracleClientGenerator(rPack,mPack,packageName).execute("dev");
		
		
	
	}

	private void execute(String pConfKey) {
		
        Connection connection = null;
        try {
        	connection = ConnectionProvider.getOracleConnection(pConfKey);
        	
        	
        	DatabaseMetaData dbMetaData = connection.getMetaData();
//        	
//        	ResultSet rsProc = dbMetaData.getProcedures(connection.getCatalog(), "CUSTOMER", null );
//        	System.out.println("procedures:");
//        	while(rsProc.next()){
//				 String procedureCatalog     = rsProc.getString(1);
//				 if("WEB_HLTPRV_UTILS".equals(procedureCatalog)){
//	        		for(int i=1;i<7;i++){
//	        			System.out.println(i + " = " + rsProc.getString(i));
//	        		}
//				 }
//        	}
        	
        	
        	ResultSet rs = dbMetaData.getFunctions(connection.getCatalog(), "CUSTOMER", null );
        	System.out.println("functions:");
			while(rs.next()) {
				 String procedureCatalog     = rs.getString(1);
				 if(packageName.equals(procedureCatalog)){
					 
					 ResultSet functionColumns = dbMetaData.getProcedureColumns(connection.getCatalog(), "CUSTOMER", rs.getString("FUNCTION_NAME") , null );
					 System.out.println(rs.getString("FUNCTION_NAME"));
					 while(functionColumns.next()){
						 if(functionColumns.getString("COLUMN_NAME") == null){
							 String typeName = functionColumns.getString("TYPE_NAME");
							 if(typeName != null){
								 if(typeName.contains("NUMBER")){
									 FUNCTIONS_MAP.put(rs.getString("FUNCTION_NAME"), BigDecimal.class);
								 }else if(typeName.contains("VARCHAR")){
									 FUNCTIONS_MAP.put(rs.getString("FUNCTION_NAME"), String.class);
								 }else if(typeName.contains("DATE")){
									 FUNCTIONS_MAP.put(rs.getString("FUNCTION_NAME"), Date.class);
								 }else if(typeName.contains("BLOB")){
									 FUNCTIONS_MAP.put(rs.getString("FUNCTION_NAME"), Byte[].class);
								 }else if(typeName.contains("BOOLEAN")){
									 FUNCTIONS_MAP.put(rs.getString("FUNCTION_NAME"), Boolean.class);
								 }else{
									 throw new Exception(rs.getString("FUNCTION_NAME")  + " invalid return type , function. " + typeName);
								 }
							 }
							 break;
						 }
//						 System.out.println(functionColumns.getString("COLUMN_NAME") + " : " + functionColumns.getShort("COLUMN_TYPE") + " " + );
					 }
				 }
			}
        	 
        	 
        	createPackageTypes(packageName, connection);
        	String packageClassName = createPackage("CUSTOMER", packageName, connection);
        	createRunner(packageClassName);
        	
        	
        	codeModel.build(ROOT_FOLDER);
        }catch(Exception e){
        	e.printStackTrace();
        }finally{
        	try {
				connection.close();
			} catch (Exception e) {
			}
        }
	}

	private void createRunner(String packageClassName) throws JClassAlreadyExistsException {
		JDefinedClass dc = codeModel._class(runtimePackage + "Runner");
		JMethod mainMethod = dc.method(JMod.PUBLIC | JMod.STATIC, codeModel.VOID, "main");
		mainMethod.param(String[].class, "args");
		mainMethod.body().directStatement("javax.xml.ws.Endpoint.publish(\"http://localhost:8080/ora2wsruntime/"+packageClassName.toLowerCase()+"\", new "+packageClassName+"());");
	}

	/**
	 * @param packageName
	 * @param connection
	 * @throws Exception 
	 */
	@SuppressWarnings({ "unused", "rawtypes" })
	private String createPackage(String schema , String packageName, Connection connection) throws Exception {
		
		packageName = packageName.toUpperCase(Locale.ENGLISH);
		String packageClassName = convertTypeNameToJavaClassName(packageName);
		JDefinedClass dc = codeModel._class(runtimePackage + packageClassName);
		JAnnotationUse wsClassAnno = dc.annotate(codeModel.ref(WebService.class));
		wsClassAnno.param("name", packageClassName);
		wsClassAnno.param("serviceName",packageClassName + "Service");
		
		JFieldVar field = dc.field(JMod.PRIVATE, codeModel.ref(Connection.class), "oracleConnection" , null);
		
		JAnnotationUse suppressWarnings = field.annotate(codeModel.ref(SuppressWarnings.class));
		suppressWarnings.param("value","unused");
		
		JMethod constructor = dc.constructor(JMod.PUBLIC);
		
		dc.constructor(JMod.PUBLIC); // def constract
		
		constructor.param(Connection.class, "pOracleConnection");
		constructor.body().assign(JExpr._this().ref ("oracleConnection"), JExpr.ref ("pOracleConnection"));
		JAnnotationUse packageAnnotation = dc.annotate(codeModel.ref(Ora2WsPackage.class));
		packageAnnotation.param("name", packageName);
		packageAnnotation.param("schema", schema);
		
		addClassComments(dc);
		
		Statement stmt = connection.createStatement();
		
		ResultSet rs = stmt.executeQuery("SELECT OWNER , PROCEDURE_NAME  FROM SYS.DBA_PROCEDURES  WHERE OBJECT_TYPE = 'PACKAGE'  AND  OBJECT_NAME = '" + packageName + "' AND  PROCEDURE_NAME IS NOT NULL");
		while(rs.next()){
			String ownr = rs.getString("OWNER");
			String name = rs.getString("PROCEDURE_NAME");
			
			System.out.println(ownr + "." + name);
			
			Statement argStmt = connection.createStatement();
			
			ResultSet argRs = argStmt.executeQuery("SELECT ARGUMENT_NAME, POSITION, TYPE_NAME, DATA_TYPE, IN_OUT FROM SYS.ALL_ARGUMENTS WHERE PACKAGE_NAME = '"+packageName+"'  AND OBJECT_NAME = '"+ name +"' AND ARGUMENT_NAME IS NOT NULL ORDER BY POSITION");
			
			Ora2WsMethodType methodType = Ora2WsMethodType.PROCEDURE;
			
			
			String procedureReturnClass = runtimePackage + formatForGetterSetter(name) + "Response";
			JClass methotReturnType = codeModel.ref(procedureReturnClass);
			Class methodReturnClass = null;
			
			if(FUNCTIONS_MAP.containsKey(name)){
				methodType = Ora2WsMethodType.FUNCTION;
				methodReturnClass = FUNCTIONS_MAP.get(name);
			}
			
			
			JMethod method = null;
			boolean customReturnType = false;
			if(methodReturnClass == null){
				method = dc.method(JMod.PUBLIC, methotReturnType, name.toLowerCase(Locale.ENGLISH));
			}else{
				method = dc.method(JMod.PUBLIC, methodReturnClass, name.toLowerCase(Locale.ENGLISH));
//				method.body()._return(JExpr.direct("null"));
			}
			
//			@WebMethod(operationName="gethltprvbreobj-oper", action="gethltprvbreobj-act")
			
			JAnnotationUse webMethodAnnotation = method.annotate(codeModel.ref(WebMethod.class));
			webMethodAnnotation.param("operationName",name.toLowerCase(Locale.ENGLISH) + "-oper");
			webMethodAnnotation.param("action", name.toLowerCase(Locale.ENGLISH) + "-act");
			
			
			JAnnotationUse methodAnnotation = method.annotate(codeModel.ref(Ora2WsMethod.class));
			methodAnnotation.param("name", name);
			methodAnnotation.param("type", methodType);
			
			if(methodType == Ora2WsMethodType.FUNCTION) {
				methodAnnotation.param("isCustomReturnType", false); // TODO impl. gerekiyor.
				methodAnnotation.param("returnType", methodReturnClass);
			}
			
			
			int parameterCount = 0;
			
			HashMap<String, JClass> responseElements = new HashMap<String, JClass>();
			JArray newArray = JExpr.newArray(codeModel.ref(Object.class));
			
			while(argRs.next()){
				String argName 		= argRs.getString("ARGUMENT_NAME");
				int position 		= argRs.getInt("POSITION");
				String typeName 	= argRs.getString("TYPE_NAME");
				String dataType		= argRs.getString("DATA_TYPE");
				String inOut 		= argRs.getString("IN_OUT");
				System.out.println("\t" + argName + " " + typeName + " " + dataType + " " + inOut);
				
				
				String paramName = "p_" + argName.toLowerCase(Locale.ENGLISH);
				
				JVar param = null;
				JClass fieldClass = null;
				boolean customType = false;
				if(typeName == null){
					 if(dataType.contains("NUMBER")){
						 fieldClass = codeModel.ref(BigDecimal.class);
					 }else if(dataType.contains("VARCHAR")){
						 fieldClass = codeModel.ref(String.class);
					 }else if(dataType.contains("DATE")){
						 fieldClass = codeModel.ref(Date.class);
					 }else if(dataType.contains("REF CURSOR")){
//						 JType mapType = CODE_MODEL.ref(HashMap.class).narrow(CODE_MODEL.ref(String.class), CODE_MODEL.ref(Object.class));
						 JType mapType = codeModel.ref(Ora2WsListWrapper.class);
						 fieldClass = codeModel.ref(List.class).narrow(mapType);
					 }else{
//						 throw new Exception(name  + " invalid argument type . " + dataType);
						 System.err.println(name  + " invalid argument type . " + dataType);
						 fieldClass = codeModel.ref(String.class);
					 }
					
				}else{
					fieldClass = codeModel.ref(modelPackage + convertTypeNameToJavaClassName(clearSchemaName(typeName)));
					customType = true;
				}
				
				
				param = method.param(fieldClass, paramName);
				
				JAnnotationUse parameterAnnotation = param.annotate(codeModel.ref(Ora2WsArgument.class));
				parameterAnnotation.param("clazz",  fieldClass);
				parameterAnnotation.param("name",  argName);
				parameterAnnotation.param("position", position);
				parameterAnnotation.param("isCustomType", customType);
				
				JAnnotationUse webServiceParam = param.annotate(codeModel.ref(WebParam.class));
				webServiceParam.param("name", paramName);
				
				boolean addToResponseElements = false;
				Ora2WsArgumentType argInOutType = null;
				if("OUT".equals(inOut)){
					argInOutType = Ora2WsArgumentType.OUT;
					addToResponseElements = true;
				}else if("IN/OUT".equals(inOut)){
					argInOutType = Ora2WsArgumentType.IN_OUT;
					addToResponseElements = true;
				}else if("IN".equals(inOut)){
					argInOutType = Ora2WsArgumentType.IN;
				}else{
					throw new Exception("invalid arg input type method :" + name + ", arg name:" + argName);
				}
				
				parameterAnnotation.param("type",  argInOutType);
				
				parameterCount++;
				
				if(addToResponseElements){
					responseElements.put(argName.toLowerCase(Locale.ENGLISH), fieldClass);
				}
				
				newArray.add(JExpr.ref(paramName));
				
			}
			
			StringBuffer buffy = new StringBuffer();
			for(int i=0;i<parameterCount;i++){
				if(i>0){
					buffy.append(" , ");
				}
				buffy.append("?");
				
			}
			
			if(methodType == Ora2WsMethodType.FUNCTION){
				methodAnnotation.param("preparedStatementForCall","{? = call " + packageName + "." + name + "(" + buffy.toString() + ")}");
				
				JVar procedureParametersVar = method.body().decl(codeModel.ref(Object[].class), "procedureParameters");
				JTryBlock _try = method.body()._try();
				JBlock body = _try.body();
				JCatchBlock _catch = _try._catch(codeModel.ref(Exception.class));
				
				_catch.body().directStatement("throw _x;");
				
				method._throws(codeModel.ref(Exception.class));
				
				
				procedureParametersVar.init(newArray);
				
				JInvocation invokeProcedure = codeModel.ref("org.borademir.ora2ws.runtime.JFOracleProcedureCaller").staticInvoke("callProcedure")
						.arg(JExpr.dotclass(dc))
						.arg(name.toLowerCase(Locale.ENGLISH))
						.arg(JExpr.ref("procedureParameters"))
						.arg(JExpr.dotclass(codeModel.ref(methodReturnClass)))
						.arg(JExpr.ref("pTargetDatabase"));
				
//				body.add(invokeProcedure);
				body._return(invokeProcedure);
				
				
//				method.body()._return(JExpr.direct("null"));
				
				
			}else{
				methodAnnotation.param("preparedStatementForCall","{call " + packageName + "." + name + "(" + buffy.toString() + ")}");
				JDefinedClass responseClass = codeModel._class(procedureReturnClass);
				Iterator<String> elementsKeyIt = responseElements.keySet().iterator();
				while (elementsKeyIt.hasNext()) {
					String fieldName = (String) elementsKeyIt.next();
					JClass fieldClass = responseElements.get(fieldName);
					JFieldVar respField = responseClass.field(JMod.PRIVATE, fieldClass, fieldName , null);
					generateGetterSetter(responseClass, respField);
				}
				
				JVar procedureParametersVar = method.body().decl(codeModel.ref(Object[].class), "procedureParameters");
				
				JTryBlock _try = method.body()._try();
				JBlock body = _try.body();
				JCatchBlock _catch = _try._catch(codeModel.ref(Exception.class));
				
				_catch.body().directStatement("throw _x;");
				
				method._throws(codeModel.ref(Exception.class));
				
				procedureParametersVar.init(newArray);
				
				JInvocation invokeProcedure = codeModel.ref("org.borademir.ora2ws.runtime.JFOracleProcedureCaller").staticInvoke("callProcedure")
						.arg(JExpr.dotclass(dc))
						.arg(name.toLowerCase(Locale.ENGLISH))
						.arg(JExpr.ref("procedureParameters"))
						.arg(JExpr.dotclass(codeModel.ref(procedureReturnClass)))
						.arg(JExpr.ref("pTargetDatabase"));
				
//				body.add(invokeProcedure);
				body._return(invokeProcedure);
				
				
//				method.body()._return(JExpr.direct("null"));
			}
		}
		
		return packageClassName;
		
	}

	/**
	 * @param typeName
	 * @param connection
	 * @throws SQLException
	 * @throws Exception
	 * @throws IOException
	 */
	private void createPackageTypes(String packageName, Connection connection) throws SQLException, Exception, IOException {
		Statement stmt = connection.createStatement();
		
		ResultSet rs = stmt.executeQuery("SELECT DISTINCT DATA_TYPE, TYPE_NAME , OWNER FROM SYS.ALL_ARGUMENTS WHERE PACKAGE_NAME = '"+ packageName +"' AND TYPE_NAME IS NOT NULL  ");
		while(rs.next()){
			String type = rs.getString("DATA_TYPE");
			String name = rs.getString("TYPE_NAME");
			String ownr = rs.getString("OWNER");
			
			boolean isArray = "TABLE".equals(type);
			
			try {
				if(isArray){
					try {
						ArrayDescriptor arrayDesc = ArrayDescriptor.createDescriptor(name, connection);
						String arrayTypeName = arrayDesc.getBaseName();
						processArray(connection, 1, null, arrayDesc, arrayTypeName, "dataList");
					} catch (Exception e) {
						throw new Exception(e.getMessage() + " for type : " + name);
					}
				}else{
					process(ownr + "." + name, connection,1,false);
				}
			} catch (Exception e) {
				throw e;
			}
			
		}
	}
	
	/**
	 * @return
	 */
	private static File calculateSourceFolder() {
		String parent = new File(new File(new File(OracleClientGenerator	.class.getResource("/").getFile()).getParent()).getParent()).getParent();
		return new File(parent + File.separator + "ora2ws-runtime" + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator );
	}

	/**
	 * @param typeName
	 * @param connection
	 * @throws Exception 
	 */
	void process(String typeName, Connection connection,int pLevel,boolean isArray) throws Exception {
		final StructDescriptor structDescriptor = StructDescriptor.createDescriptor(typeName.toUpperCase(), connection);
		final ResultSetMetaData metaData = structDescriptor.getMetaData();
		if(owner == null){
			structDescriptor.getSchemaName();
		}
		printTab(pLevel-1);
		JDefinedClass dc = null;
		try {
			if(!isArray){
				String clearTypeName = convertTypeNameToJavaClassName(clearSchemaName(typeName));
				String schemaName 	 = extractSchema(typeName);
				dc = codeModel._class(modelPackage + clearTypeName);
				dc._extends(Ora2WsBaseModel.class);
				addClassAnnotations(dc, clearSchemaName(typeName), schemaName,Types.STRUCT);
				addClassComments(dc);
			}else{
	
			}
		} catch (JClassAlreadyExistsException e) {
			System.err.println(modelPackage + typeName + " is already exists");
		}
		System.out.println(typeName + "(" + structDescriptor.getInternalTypeCode() + ")" + (isArray ? "[]" : "") + " :");
		for(int i=1 ; i<= metaData.getColumnCount(); i++){
			String colType = metaData.getColumnTypeName(i);
			if(isArray(metaData,i)){
				ArrayDescriptor arrayDesc = ArrayDescriptor.createDescriptor(colType, connection);
				String arrayTypeName = arrayDesc.getBaseName();
				String columnName = metaData.getColumnName(i);
				processArray(connection, pLevel, dc, arrayDesc, arrayTypeName, columnName);
			}else if(isStruct(metaData, i,"")){
				if(dc != null){
					JClass fieldClass = codeModel.ref(convertTypeNameToJavaClassName(clearSchemaName(colType)));
					JFieldVar field = dc.field(JMod.PRIVATE, fieldClass, formatFieldName(metaData.getColumnName(i)) , null);
					generateGetterSetter(dc, field);
					addFieldAnnotations( fieldClass, field , Ora2WsType.TYPE,0);
				}
				process(colType, connection,pLevel+1,false);
			}else{
				printMetaColumn( metaData, i ,pLevel);
				if(dc != null){
					JFieldVar field = dc.field(JMod.PRIVATE, codeModel.ref(metaData.getColumnClassName(i)), formatFieldName(metaData.getColumnName(i)) , null);
					
					JClass fieldClass = codeModel.ref(metaData.getColumnClassName(i));
					
			        generateGetterSetter(dc, field);
			        
			        int fieldLenght = metaData.getColumnDisplaySize(i);
					
					addFieldAnnotations(fieldClass, field , Ora2WsType.PRIMITIVE,fieldLenght);
			        
			        
				}
			}
		}
	}

	/**
	 * @param connection
	 * @param pLevel
	 * @param dc
	 * @param arrayDesc
	 * @param arrayTypeName
	 * @param columnName
	 * @throws Exception 
	 */
	private void processArray(Connection connection, int pLevel, JDefinedClass dc, ArrayDescriptor arrayDesc,
			String arrayTypeName, String columnName)
					throws Exception {
		JClass narrowedClass = codeModel.ref(convertTypeNameToJavaClassName(clearSchemaName(arrayTypeName)));
		String arrayClass = null;
		try {
			arrayClass = modelPackage + convertTypeNameToJavaClassName(clearSchemaName(arrayDesc.getName()));
			JDefinedClass arrayDefinedClass = codeModel._class(arrayClass);
			arrayDefinedClass._extends(Ora2WsBaseArray.class);
			addClassAnnotations(arrayDefinedClass, clearSchemaName(arrayDesc.getName()), extractSchema(arrayDesc.getName()) , Types.ARRAY);
			
			
//			JType hashMapType = CODE_MODEL.ref(HashMap.class).narrow(
//					CODE_MODEL.ref(String.class),
//					CODE_MODEL.ref(Object.class)
//			);
			
//			JMethod generateOracleArrayObjectFromFielsMethod = arrayDefinedClass.method(JMod.PUBLIC, hashMapType, "generateOracleArrayObjectFromFiels");
//			generateOracleArrayObjectFromFielsMethod.param(Connection.class, "pOracleConnection");
//			generateOracleArrayObjectFromFielsMethod.body()._return(JExpr.direct("null"));
			
			
			JAnnotationUse jClassAnnotation = arrayDefinedClass.annotate(codeModel.ref(Ora2WsTableOf.class));
			jClassAnnotation.param("clazz",  narrowedClass);
			
			JClass listClass = codeModel.ref(List.class).narrow(narrowedClass);
			
			JFieldVar field = arrayDefinedClass.field(JMod.PRIVATE, listClass, formatFieldName("DATALIST") , null);
			generateGetterSetter( arrayDefinedClass, field);
			
			addFieldAnnotations(narrowedClass, field, Ora2WsType.ARRAY,0);
			
			
		} catch (JClassAlreadyExistsException e) {
			System.err.println(modelPackage + arrayDesc.getName() + " is already exists");
		}
		if(dc != null){
			JFieldVar field = dc.field(JMod.PRIVATE, codeModel.ref(arrayClass), formatFieldName(columnName) , null);
			generateGetterSetter( dc, field);
			
			addFieldAnnotations(codeModel.ref(arrayClass), field, Ora2WsType.ARRAY,0);
		}
		process(arrayTypeName, connection,pLevel+1,false);
	}

	/**
	 * @param clearSchemaName
	 * @return
	 */
	private String convertTypeNameToJavaClassName(String clearSchemaName) {
		if(clearSchemaName != null){
			clearSchemaName = clearSchemaName.toLowerCase(Locale.ENGLISH);
			
			List<Integer> underScoreList = new ArrayList<Integer>();
			for(int i=0;i<clearSchemaName.length();i++){
				String str = String.valueOf(clearSchemaName.charAt(i));
				if(str.equals("_")){
					underScoreList.add(i);
				}
			}
			
			
			StringBuffer buf = new StringBuffer(clearSchemaName);
			for(int underScoreIndex : underScoreList){
				buf.replace(underScoreIndex+1, underScoreIndex+2, clearSchemaName.substring(underScoreIndex+1,underScoreIndex+2).toUpperCase(Locale.ENGLISH));
			}
			
			buf.replace(0, 1, buf.substring(0,1).toUpperCase(Locale.ENGLISH));
			return buf.toString().replaceAll("_", "");
		}
		return clearSchemaName;
	}

	/**
	 * @param dc
	 */
	private void addClassComments(JDefinedClass dc) {
		JDocComment jDocComment = dc.javadoc();
		jDocComment.add("Auto Generated Source Code With JCodeModel with {@link JCodeModel}\n");
		jDocComment.add("@author\t\tBora.Demir\n");
		jDocComment.add("@date\t\t" + PRETTY_DATE_FORMATTER.format(new Date()) + "\n");
		jDocComment.add("@company\t\tJFORCE Yazilim Teknolojileri");
		
	}

	/**
	 * @param metaData
	 * @param i
	 * @param narrowedClass
	 * @param field
	 * @throws SQLException
	 */
	private void addFieldAnnotations(JClass narrowedClass,JFieldVar field, Ora2WsType pOracleType,int pLen) throws SQLException {
		JAnnotationUse fieldAnnotation = field.annotate(codeModel.ref(Ora2WsField.class));
		fieldAnnotation.param("name",  field.name().toUpperCase(Locale.ENGLISH));
		fieldAnnotation.param("clazz", narrowedClass);
		fieldAnnotation.param("type",  pOracleType);
		
		if(pLen > 0){
			JAnnotationUse lenAnnotation = field.annotate(codeModel.ref(Ora2WsFieldLength.class));
			lenAnnotation.param("length",  pLen);
		}
	}

	/**
	 * @param dc
	 * @param clearTypeName
	 * @param schemaName
	 */
	private void addClassAnnotations(JDefinedClass dc, String clearTypeName, String schemaName, int pSqlType) {
		JAnnotationUse jClassAnnotation = dc.annotate(codeModel.ref(Ora2WsClass.class));
		jClassAnnotation.param("name",  clearTypeName);
		jClassAnnotation.param("schema", schemaName);
		jClassAnnotation.param("sqlType", pSqlType);
	}


	/**
	 * @param metaData
	 * @param dc
	 * @param i
	 * @param field
	 * @throws SQLException
	 */
	private void generateGetterSetter(JDefinedClass dc, JFieldVar field)
			throws SQLException {
		JMethod getterMethod = dc.method(JMod.PUBLIC, field.type(), "get" + formatForGetterSetter(formatFieldName(field.name())));
		getterMethod.body()._return(field);
		
		JMethod setterMethod = dc.method(JMod.PUBLIC, codeModel.VOID, "set" + formatForGetterSetter(formatFieldName(field.name())));
		String parameterNameForSetter = "p_" + formatFieldName(field.name());
		setterMethod.param(field.type(), parameterNameForSetter);
		setterMethod.body().assign(JExpr._this().ref (formatFieldName(field.name())), JExpr.ref (parameterNameForSetter));
	}
	
	
	/**
	 * @param formatFieldName
	 * @return
	 */
	private String formatForGetterSetter(String formatFieldName) {
		if(formatFieldName != null){
			return formatFieldName.substring(0, 1).toUpperCase() + formatFieldName.substring(1).toLowerCase(Locale.ENGLISH);
		}
		return null;
	}

	/**
	 * @param columnName
	 * @return
	 */
	private String formatFieldName(String columnName) {
		if(columnName != null){
			return columnName.toLowerCase(Locale.ENGLISH);
		}
		return null;
	}

	/**
	 * @param typeName
	 * @return
	 */
	private String clearSchemaName(String typeName) {
		if(typeName != null){
			return typeName.substring(typeName.indexOf(".")+1);
		}
		return null;
	}
	
	/**
	 * @param typeName
	 * @return
	 */
	private String extractSchema(String typeName) {
		if(typeName != null){
			return typeName.substring(0,typeName.indexOf("."));
		}
		return null;
	}

	/**
	 * @param pLevel
	 */
	private void printTab(int pLevel) {
		for(int ind=0;ind<pLevel;ind++){
			System.out.print("\t");
		}
	}
	
	/**
	 * @param metaData
	 * @param i
	 * @throws SQLException
	 */
	private void printMetaColumn(final ResultSetMetaData metaData,int i, int pLevel) throws SQLException {
		printTab(pLevel);
		System.out.println(metaData.getColumnName(i) + " - " + metaData.getColumnTypeName(i) + "(" + metaData.getColumnDisplaySize(i) + ")" + " - " +  metaData.getColumnClassName(i));
	}
	
	/**
	 * @param metaData
	 * @param i
	 * @param string
	 * @return
	 * @throws SQLException 
	 */
	private boolean isStruct(ResultSetMetaData metaData, int i, String string) throws SQLException {
		return "oracle.jdbc.OracleStruct".equals(metaData.getColumnClassName(i));
	}

	/**
	 * @param metaData
	 * @param i
	 * @return
	 * @throws SQLException 
	 */
	private boolean isArray(ResultSetMetaData metaData, int i) throws SQLException {
		return "oracle.jdbc.OracleArray".equals(metaData.getColumnClassName(i));
	}

}
