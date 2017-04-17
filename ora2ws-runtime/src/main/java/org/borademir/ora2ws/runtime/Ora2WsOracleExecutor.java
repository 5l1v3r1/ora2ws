/**
 * 
 */
package org.borademir.ora2ws.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import oracle.jdbc.internal.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

import org.borademir.ora2ws.annotation.Ora2WsArgument;
import org.borademir.ora2ws.annotation.Ora2WsClass;
import org.borademir.ora2ws.annotation.Ora2WsField;
import org.borademir.ora2ws.annotation.Ora2WsMethod;
import org.borademir.ora2ws.annotation.Ora2WsTableOf;
import org.borademir.ora2ws.db.util.ConnectionProvider;
import org.borademir.ora2ws.model.Ora2WsArgumentType;
import org.borademir.ora2ws.model.Ora2WsKeyValue;
import org.borademir.ora2ws.model.Ora2WsListWrapper;
import org.borademir.ora2ws.model.Ora2WsMethodType;
import org.borademir.ora2ws.model.Ora2WsOuputMetadata;
import org.borademir.ora2ws.model.Ora2WsType;

/**
 * @author 			Bora.Demir
 * @date   			Oct 2, 2015 11:40:48 AM
 * @description		
 */
public class Ora2WsOracleExecutor {
	
	private static final String DB_CONF_KEY = "dev";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends Object> T callProcedure(Class pPackageClass , String pProcedureOrFunctionName, Object[] paramaterObjects , Class<T> procedureReturnClass) throws Exception {
		Connection connection = null;

		//			ResultSetMetaData metaData = null;
		CallableStatement cs = null;

		try{
			connection = ConnectionProvider.getOracleConnection(DB_CONF_KEY);

			boolean isFunction = false;

			Class functionReturnClass = null;

			ArrayList<Ora2WsOuputMetadata> outputMetadatas = new ArrayList<Ora2WsOuputMetadata>();

			for(Method method : pPackageClass.getMethods()) {
				if(method.getName().equals(pProcedureOrFunctionName)){
					Ora2WsMethod methodAnnotation = method.getAnnotation(Ora2WsMethod.class);
					cs = connection.prepareCall(methodAnnotation.preparedStatementForCall());
					isFunction = methodAnnotation.type() == Ora2WsMethodType.FUNCTION;

					for(Annotation[] paramAnnotationArr : method.getParameterAnnotations()){
						if(isFunction){
							int sqlType = classToSqlType(methodAnnotation.returnType());
							cs.registerOutParameter(1, sqlType);
							functionReturnClass = methodAnnotation.returnType();
						}
						for(Annotation paramAnnotation : paramAnnotationArr){
							if(!(paramAnnotation instanceof Ora2WsArgument)){
								continue;
							}
							Ora2WsArgument arg = (Ora2WsArgument) paramAnnotation;
							int parameterPosition = arg.position();
							int parameterArrayIndex = arg.position()-1;
							if(isFunction){
								parameterPosition++;
								//									parameterArrayIndex--;
							}

							boolean outArg = arg.type() == Ora2WsArgumentType.IN_OUT || arg.type() == Ora2WsArgumentType.OUT;
							if(arg.isCustomType()){
								Ora2WsClass classAnnotation = (Ora2WsClass) arg.clazz().getAnnotation(Ora2WsClass.class);
								if(arg.type() == Ora2WsArgumentType.IN_OUT || arg.type() == Ora2WsArgumentType.OUT ){
									cs.registerOutParameter(parameterPosition,classAnnotation.sqlType(),classAnnotation.schema() + "." + classAnnotation.name());
									System.out.println("cs.registerOutParameter(" + parameterPosition + "," + classAnnotation.sqlType() + "," + classAnnotation.schema() + "." + classAnnotation.name() + ");");
									if(arg.type() == Ora2WsArgumentType.OUT){
										StructDescriptor structDescriptor = StructDescriptor.createDescriptor(classAnnotation.schema() + "." + classAnnotation.name(),connection);
										Ora2WsOuputMetadata ouputMetadata = new Ora2WsOuputMetadata();
										ouputMetadata.setPosition(parameterPosition);
										ouputMetadata.setDescriptorName(classAnnotation.name());
										ouputMetadata.setOutputType(Ora2WsType.TYPE);
										ouputMetadata.setStructDescriptor(structDescriptor);
										ouputMetadata.setParamaterInstance(paramaterObjects[parameterArrayIndex]);
										ouputMetadata.setClazz(arg.clazz());
										outputMetadatas.add(ouputMetadata);
										ouputMetadata.setResponseFieldName(arg.name().toLowerCase(Locale.ENGLISH));
									}
								}
								if(arg.type() == Ora2WsArgumentType.IN || arg.type() == Ora2WsArgumentType.IN_OUT){

									if(classAnnotation.sqlType() == Types.STRUCT){
										StructDescriptor structDescriptor = StructDescriptor.createDescriptor(classAnnotation.schema() + "." + classAnnotation.name(),connection);
										//											oracle.sql.STRUCT structParam = new oracle.sql.STRUCT(structDescriptor, connection, new HashMap());

										oracle.sql.STRUCT autoGeneratedStruct = generateStructFromObject(classAnnotation.schema() , classAnnotation.name(), connection, paramaterObjects[parameterArrayIndex]);
										if(autoGeneratedStruct == null){
											cs.setNull(arg.position(), classAnnotation.sqlType(),classAnnotation.name());
										}else{
											cs.setObject(parameterPosition, autoGeneratedStruct);
										}


										if(outArg){
											Ora2WsOuputMetadata ouputMetadata = new Ora2WsOuputMetadata();
											ouputMetadata.setPosition(parameterPosition);
											ouputMetadata.setDescriptorName(classAnnotation.name());
											ouputMetadata.setOutputType(Ora2WsType.TYPE);
											ouputMetadata.setStructDescriptor(structDescriptor);
											ouputMetadata.setParamaterInstance(paramaterObjects[parameterArrayIndex]);
											ouputMetadata.setClazz(arg.clazz());
											outputMetadatas.add(ouputMetadata);
											ouputMetadata.setResponseFieldName(arg.name().toLowerCase(Locale.ENGLISH));
										}

									}else if(classAnnotation.sqlType() == Types.ARRAY){
										Ora2WsTableOf tableOfAnnotation = (Ora2WsTableOf) arg.clazz().getAnnotation(Ora2WsTableOf.class);
										Ora2WsClass typeClassAnnotation = (Ora2WsClass) tableOfAnnotation.clazz().getAnnotation(Ora2WsClass.class);
										StructDescriptor typeStructDesc = StructDescriptor.createDescriptor(typeClassAnnotation.name(),connection);
										//											new oracle.sql.STRUCT(typeStructDesc, connection, new HashMap())
										oracle.sql.ARRAY arrayParam = new ARRAY(ArrayDescriptor.createDescriptor(classAnnotation.name(),connection), connection, new Object[] {});
										cs.setObject(parameterPosition, arrayParam);

										//											cs.setNull(arg.position(), classAnnotation.sqlType(),classAnnotation.name());
										System.out.println("cs.setObject(" + parameterPosition + "," + " arrayParam);");

										if(outArg){
											Ora2WsOuputMetadata ouputMetadata = new Ora2WsOuputMetadata();
											ouputMetadata.setPosition(parameterPosition);
											ouputMetadata.setDescriptorName(typeClassAnnotation.name());
											ouputMetadata.setOutputType(Ora2WsType.ARRAY);
											ouputMetadata.setStructDescriptor(typeStructDesc);
											ouputMetadata.setParamaterInstance(paramaterObjects[parameterArrayIndex]);
											ouputMetadata.setClazz(arg.clazz());
											ouputMetadata.setResponseFieldName(arg.name().toLowerCase(Locale.ENGLISH));
											outputMetadatas.add(ouputMetadata);
										}
									}
								}
							}else{
								if(arg.type() == Ora2WsArgumentType.IN_OUT || arg.type() == Ora2WsArgumentType.OUT ){
									int sqlType = classToSqlType(arg.clazz());
									cs.registerOutParameter(parameterPosition, sqlType);
									System.out.println("cs.registerOutParameter(" + parameterPosition + "," +sqlType + ");");

									Ora2WsOuputMetadata ouputMetadata = new Ora2WsOuputMetadata();
									ouputMetadata.setPosition(parameterPosition);
									ouputMetadata.setOutputType(Ora2WsType.CURSOR);
									outputMetadatas.add(ouputMetadata);
									ouputMetadata.setResponseFieldName(arg.name().toLowerCase(Locale.ENGLISH));


								}
								if(arg.type() == Ora2WsArgumentType.IN || arg.type() == Ora2WsArgumentType.IN_OUT){
									if(arg.clazz().equals(BigDecimal.class)){
										cs.setBigDecimal(parameterPosition, (BigDecimal) paramaterObjects[parameterArrayIndex]);
										System.out.println("cs.setBigDecimal(" + parameterPosition + ", null);");
									}else if(arg.clazz().equals(String.class)){
										cs.setString(parameterPosition, (String) paramaterObjects[parameterArrayIndex]);
									}else if(arg.clazz().equals(Date.class)){
										cs.setDate(parameterPosition, (java.sql.Date) paramaterObjects[parameterArrayIndex]);
									}
								}
							}
						}
					}
				}
			}

			if(isFunction){
				Ora2WsOuputMetadata ouputMetadata = new Ora2WsOuputMetadata();
				ouputMetadata.setOutputType(Ora2WsType.PRIMITIVE);
				ouputMetadata.setClazz(functionReturnClass);
				ouputMetadata.setPosition(1);
				outputMetadatas.add(ouputMetadata);
			}
			cs.execute();

			//				HltprvProvisionReqTyp param = (HltprvProvisionReqTyp) paramaterObjects[1];
			//				param.setMedicalinfo(null);
			Object newInstance = null;
			if(!isFunction){
				newInstance = procedureReturnClass.newInstance();
			}

			for(Ora2WsOuputMetadata outputMetadata : outputMetadatas){
				if(isFunction){
					//						return (T) generateObjectFromStruct(cs, outputMetadata,connection,newInstance);
					return (T) cs.getObject(1);
				}else{
					generateObjectFromStruct(cs, outputMetadata,connection,newInstance);
				}
			}

			System.out.println("done " + pProcedureOrFunctionName);
			return (T) newInstance;
		}catch(SQLException e){
			throw e;
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}finally{
			try {
				connection.close();
			} catch (Exception e) {
			}
		}
	}
	/**
	 * @param cs
	 * @param outputMetadata
	 * @param newInstance 
	 * @throws SQLException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	private static Object generateObjectFromStruct(CallableStatement cs, Ora2WsOuputMetadata outputMetadata , Connection connection, Object responseInstance)
			throws SQLException, IllegalAccessException, InvocationTargetException, InstantiationException {
		if(outputMetadata.getOutputType() == Ora2WsType.ARRAY){
			Object[] data = (Object[]) ((Array) cs.getObject(outputMetadata.getPosition())).getArray();
			if(outputMetadata.getParamaterInstance() == null){
				outputMetadata.setParamaterInstance(outputMetadata.getClazz().newInstance());
			}

			ArrayList dataList = new ArrayList();
			for(Object tmp : data) {
				Struct row = (Struct) tmp;
				Ora2WsTableOf tableOfAnnotation = (Ora2WsTableOf) outputMetadata.getClazz().getAnnotation(Ora2WsTableOf.class);
				dataList.add(parseStruct(row ,outputMetadata.getStructDescriptor(), tableOfAnnotation.clazz().newInstance(),connection));
				System.out.println("ARRAY OUT END");
			}

			for(Method method : outputMetadata.getParamaterInstance().getClass().getMethods()){
				if(method.getName().equals("setDatalist")){
					method.invoke(outputMetadata.getParamaterInstance(), dataList);
					break;
				}
			}

		}else if(outputMetadata.getOutputType() == Ora2WsType.TYPE){
			Struct row = (Struct) cs.getObject(outputMetadata.getPosition());
			int idx = 1;

			if(row != null){
				if(outputMetadata.getParamaterInstance() == null){
					outputMetadata.setParamaterInstance(outputMetadata.getClazz().newInstance());
				}
				Object parsedObject = parseStruct(row,outputMetadata.getStructDescriptor() ,outputMetadata.getParamaterInstance() , connection);
				System.out.println("STRUCT OUT END");
			}
		}else if(outputMetadata.getOutputType() == Ora2WsType.CURSOR){
			List<Ora2WsListWrapper> p_recordset = new ArrayList<Ora2WsListWrapper>();
			ResultSet rs = (ResultSet)cs.getObject(outputMetadata.getPosition());
			if(rs != null){
				ResultSetMetaData rsMetaData = rs.getMetaData();
				while (rs.next()) {
					List<Ora2WsKeyValue> record = new ArrayList<Ora2WsKeyValue>();
					for(int i=1;i<=rsMetaData.getColumnCount();i++){
						Ora2WsKeyValue keyValuePair = new Ora2WsKeyValue();
						keyValuePair.setName(rsMetaData.getColumnName(i));
						if(rs.getMetaData().getColumnType(i) == Types.BLOB){
							Blob blob = rs.getBlob(i);
							int blobLength = (int) blob.length();  
							byte[] blobAsBytes = blob.getBytes(1, blobLength);
							blob.free();
							keyValuePair.setValue(blobAsBytes);
						}else{
							keyValuePair.setValue(rs.getObject(i));
						}
						record.add(keyValuePair);
					}
					System.out.println("****");
					Ora2WsListWrapper listWrapper = new Ora2WsListWrapper();
					listWrapper.setValues(record);
					p_recordset.add(listWrapper);
				}
				outputMetadata.setParamaterInstance(p_recordset);
			}
		}else{
			return cs.getObject(outputMetadata.getPosition());

		}
		if(responseInstance != null){
			for(Method method : responseInstance.getClass().getMethods()){
				if(method.getName().toLowerCase(Locale.ENGLISH).equals("set" + outputMetadata.getResponseFieldName())){
					method.invoke(responseInstance, outputMetadata.getParamaterInstance());
					break;
				}
			}
		}

		return responseInstance;
	}
	/**
	 * @param dataList
	 * @param values
	 * @param tableOfAnnotation
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws SQLException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object parseStruct(Struct row, StructDescriptor structDescriptor,	Object arrayTypeClassInstance, Connection connection)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, SQLException {


		int idx = 1;
		HashMap<String, Object> values = new HashMap<String, Object>();
		for(Object attribute : row.getAttributes()) {
			System.out.println(structDescriptor.getMetaData().getColumnName(idx) + " = " + attribute);
			values.put(structDescriptor.getMetaData().getColumnName(idx), attribute);
			++idx;
		}

		//			Object objectElement = arrayTypeClass.newInstance();
		for(Field attrFields : arrayTypeClassInstance.getClass().getDeclaredFields()){
			attrFields.setAccessible(true);
			Ora2WsField fieldAnnotation = attrFields.getAnnotation(Ora2WsField.class);
			if(fieldAnnotation == null){
				continue;
			}

			if(fieldAnnotation.type() == Ora2WsType.PRIMITIVE){
				if(values.containsKey(fieldAnnotation.name())){
					String setterMethodName = "set" + attrFields.getName().substring(0,1).toUpperCase(Locale.ENGLISH) + attrFields.getName().substring(1);
					for(Method method : arrayTypeClassInstance.getClass().getMethods()){
						if(method.getName().equals(setterMethodName)){
							method.invoke(arrayTypeClassInstance,values.get(fieldAnnotation.name()));
							break;
						}
					}
				}
			}else if(fieldAnnotation.type() == Ora2WsType.TYPE){
				if(values.containsKey(fieldAnnotation.name())){
					String setterMethodName = "set" + attrFields.getName().substring(0,1).toUpperCase(Locale.ENGLISH) + attrFields.getName().substring(1);
					for(Method method : arrayTypeClassInstance.getClass().getMethods()){
						if(method.getName().equals(setterMethodName)){
							Object value = values.get(fieldAnnotation.name());
							if(value == null){
								continue;
							}
							if(value instanceof STRUCT){
								Struct structRow = (Struct) value;
								Ora2WsClass structTypeClassAnnotation = (Ora2WsClass) fieldAnnotation.clazz().getAnnotation(Ora2WsClass.class);
								if(structTypeClassAnnotation == null){
									throw new IllegalArgumentException(fieldAnnotation.name() + " attiribute must be annotated with JFOracleClass" );
								}
								Object parsedObject = parseStruct(structRow, StructDescriptor.createDescriptor(structTypeClassAnnotation.schema() +"." + structTypeClassAnnotation.name(), connection), fieldAnnotation.clazz().newInstance(),connection);
								method.invoke(arrayTypeClassInstance,parsedObject);
							}else{
								throw new IllegalArgumentException(fieldAnnotation.name() + " attiribute must be struct " );
							}
							break;
						}
					}
				}
			}else if(fieldAnnotation.type() == Ora2WsType.ARRAY){
				if(values.containsKey(fieldAnnotation.name())){
					String setterMethodName = "set" + attrFields.getName().substring(0,1).toUpperCase(Locale.ENGLISH) + attrFields.getName().substring(1);
					for(Method method : arrayTypeClassInstance.getClass().getMethods()){
						if(method.getName().equals(setterMethodName)){
							Object value = values.get(fieldAnnotation.name());
							if(value == null){
								continue;
							}
							if(value instanceof ARRAY){
								ARRAY arrayRow = (ARRAY) value;
								Ora2WsClass arrayTypeClassAnnotation = (Ora2WsClass) fieldAnnotation.clazz().getAnnotation(Ora2WsClass.class);
								if(arrayTypeClassAnnotation == null){
									throw new IllegalArgumentException(fieldAnnotation.name() + " attiribute must be annotated with JFOracleClass" );
								}
								Ora2WsTableOf tableOfAnnotation = (Ora2WsTableOf) fieldAnnotation.clazz().getAnnotation(Ora2WsTableOf.class);
								if(tableOfAnnotation == null){
									throw new IllegalArgumentException(fieldAnnotation.name() + " attiribute must be annotated with JFOracleTableOf" );
								}
								Ora2WsClass structTypeClassAnnotation = (Ora2WsClass) tableOfAnnotation.clazz().getAnnotation(Ora2WsClass.class);
								if(structTypeClassAnnotation == null){
									throw new IllegalArgumentException(tableOfAnnotation.clazz() + " attiribute must be annotated with JFOracleClass" );
								}
								Object[] data =  (Object[]) arrayRow.getArray();
								ArrayList arrayElems = new ArrayList();
								for(Object arrayRowElement : data) {
									Struct structRow = (Struct) arrayRowElement;
									Object parsedObject = parseStruct(structRow, StructDescriptor.createDescriptor(structTypeClassAnnotation.schema() +"." + structTypeClassAnnotation.name(), connection), tableOfAnnotation.clazz().newInstance(),connection);
									arrayElems.add(parsedObject);
									System.out.println(parsedObject);
								}
								Object tableInstance = fieldAnnotation.clazz().newInstance();
								for(Method tblMethod : fieldAnnotation.clazz().getMethods()){
									if(tblMethod.getName().equals("setDatalist")){
										tblMethod.invoke(tableInstance , arrayElems);
										break;
									}
								}

								method.invoke(arrayTypeClassInstance, tableInstance);
								System.out.println("done");
							}else{
								throw new IllegalArgumentException(fieldAnnotation.name() + " attiribute must be array " );
							}
						}
					}
				}
			}
		}
		return arrayTypeClassInstance;
	}


	/**
	 * @param name
	 * @param connection
	 * @param paramaterObjects 
	 * @return
	 * @throws SQLException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static STRUCT generateStructFromObject(String schema , String name, Connection connection, Object objectInstance) throws SQLException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		StructDescriptor structDescriptor = StructDescriptor.createDescriptor(schema+"." + name ,connection);

		HashMap<String, Object> structParameters = new HashMap<String, Object>();

		if(objectInstance != null){
			for(Field field : objectInstance.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				Ora2WsField fieldAnnotation = field.getAnnotation(Ora2WsField.class);
				if(fieldAnnotation == null){
					continue;
				}

				if(fieldAnnotation.type() == Ora2WsType.PRIMITIVE){
					Method method = objectInstance.getClass().getMethod("get" + field.getName().substring(0,1).toUpperCase(Locale.ENGLISH) + field.getName().substring(1));
					Object value = method.invoke(objectInstance);
					structParameters.put(fieldAnnotation.name(), value);
				}else if(fieldAnnotation.type() == Ora2WsType.TYPE){
					Ora2WsClass typeAnnotation = (Ora2WsClass) fieldAnnotation.clazz().getAnnotation(Ora2WsClass.class);
					if(typeAnnotation == null){
						continue;
					}
					if(typeAnnotation.sqlType() == Types.STRUCT){
						Method method = objectInstance.getClass().getMethod("get" + field.getName().substring(0,1).toUpperCase(Locale.ENGLISH) + field.getName().substring(1));
						Object value = method.invoke(objectInstance);
						if(value != null){
							STRUCT structField = generateStructFromObject(schema,typeAnnotation.name(), connection, value);
							structParameters.put(fieldAnnotation.name(), structField);
						}
					}
				}else if(fieldAnnotation.type() == Ora2WsType.ARRAY){
					Ora2WsClass typeAnnotation = (Ora2WsClass) fieldAnnotation.clazz().getAnnotation(Ora2WsClass.class);
					Ora2WsTableOf tableOfAnnotation = (Ora2WsTableOf) fieldAnnotation.clazz().getAnnotation(Ora2WsTableOf.class);

					if(typeAnnotation == null || tableOfAnnotation == null ){
						continue;
					}

					Ora2WsClass elementClass = (Ora2WsClass) tableOfAnnotation.clazz().getAnnotation(Ora2WsClass.class);
					if(elementClass == null){
						continue;
					}
					Method method = objectInstance.getClass().getMethod("get" + field.getName().substring(0,1).toUpperCase(Locale.ENGLISH) + field.getName().substring(1));
					Object value = method.invoke(objectInstance);
					if(value != null){
						Object[] arrayStructElements = new Object[] {};;
						List dataListObject = (List) value.getClass().getMethod("getDatalist").invoke(value);
						if(dataListObject != null){
							arrayStructElements = new Object[dataListObject.size()];
							int index = 0;
							for(Object listElement : dataListObject){
								arrayStructElements[index++] = generateStructFromObject(schema,elementClass.name(), connection, listElement);
							}
							System.out.println(dataListObject.size());
						}
						oracle.sql.ARRAY arrayParam = new ARRAY(ArrayDescriptor.createDescriptor(typeAnnotation.schema() + "." + typeAnnotation.name(),connection), connection, arrayStructElements);
						structParameters.put(fieldAnnotation.name(), arrayParam);
					}


				}
			}
			oracle.sql.STRUCT structParam = new oracle.sql.STRUCT(structDescriptor, connection, structParameters);
			return structParam;
		}


		return null;
	}
	/**
	 * @param returnType
	 * @return
	 * @throws Exception 
	 */
	@SuppressWarnings("rawtypes")
	private static int classToSqlType(Class returnType) throws Exception {
		if(returnType.equals(BigDecimal.class)){
			return Types.DECIMAL;
		}else if(returnType.equals(String.class)){
			return Types.VARCHAR;
		}else if(returnType.equals(Date.class)){
			return Types.DATE;
		}else if(returnType.equals(List.class)){
			return OracleTypes.CURSOR;
		}
		throw new Exception("unknown class for sql type:" + returnType);
	}


}
