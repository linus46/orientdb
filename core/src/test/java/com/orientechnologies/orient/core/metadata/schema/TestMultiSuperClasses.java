package com.orientechnologies.orient.core.metadata.schema;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.sql.OCommandSQL;

public class TestMultiSuperClasses {
	private ODatabaseDocumentTx db;

	  @BeforeMethod
	  public void setUp() {
	    db = new ODatabaseDocumentTx("memory:" + TestMultiSuperClasses.class.getSimpleName());
	    if (db.exists()) {
	      db.open("admin", "admin");
	    } else
	      db.create();
	  }

	  @AfterMethod
	  public void after() {
	    db.close();
	  }
	  
	  @Test
	  public void testClassCreation()
	  {
		  OSchema oSchema = db.getMetadata().getSchema();

		  OClass aClass = oSchema.createAbstractClass("javaA");
		  OClass bClass = oSchema.createAbstractClass("javaB");
		  aClass.createProperty("property", OType.INTEGER);
		  bClass.createProperty("property", OType.DOUBLE);
		  OClass cClass = oSchema.createClass("javaC", aClass,bClass);
		  testClassClreationBranch(aClass, bClass, cClass);
		  oSchema.reload();
		  testClassClreationBranch(aClass, bClass, cClass);
		  oSchema = db.getMetadata().getImmutableSchemaSnapshot();
		  aClass = oSchema.getClass("javaA");
		  bClass = oSchema.getClass("javaB");
		  cClass = oSchema.getClass("javaC");
		  testClassClreationBranch(aClass, bClass, cClass);
	  }
	  
	  private void testClassClreationBranch(OClass aClass, OClass bClass, OClass cClass)
	  {
		  assertNotNull(aClass.getSuperClasses());
		  assertEquals(aClass.getSuperClasses().size(), 0);
		  assertNotNull(bClass.getSuperClassesNames());
		  assertEquals(bClass.getSuperClassesNames().size(), 0);
		  assertNotNull(cClass.getSuperClassesNames());
		  assertEquals(cClass.getSuperClassesNames().size(), 2);
		  
		  List<? extends OClass> superClasses = cClass.getSuperClasses();
		  assertTrue(superClasses.contains(aClass));
		  assertTrue(superClasses.contains(bClass));
		  assertTrue(cClass.isSubClassOf(aClass));
		  assertTrue(cClass.isSubClassOf(bClass));
		  assertTrue(aClass.isSuperClassOf(cClass));
		  assertTrue(bClass.isSuperClassOf(cClass));
		  
		  OProperty property = cClass.getProperty("property");
		  assertEquals(OType.INTEGER, property.getType());
		  property = cClass.propertiesMap().get("property");
		  assertEquals(OType.INTEGER, property.getType());
	  }
	  
	  @Test
	  public void testSql()
	  {
		  final OSchema oSchema = db.getMetadata().getSchema();

		  OClass aClass = oSchema.createAbstractClass("sqlA");
		  OClass bClass = oSchema.createAbstractClass("sqlB");
		  OClass cClass = oSchema.createClass("sqlC");
		  db.command(new OCommandSQL("alter class sqlC superclasses sqlA, sqlB")).execute();
		  oSchema.reload();
		  assertTrue(cClass.isSubClassOf(aClass));
		  assertTrue(cClass.isSubClassOf(bClass));
		  db.command(new OCommandSQL("alter class sqlC superclass sqlA")).execute();
		  oSchema.reload();
		  assertTrue(cClass.isSubClassOf(aClass));
		  assertFalse(cClass.isSubClassOf(bClass));
		  db.command(new OCommandSQL("alter class sqlC superclass +sqlB")).execute();
		  oSchema.reload();
		  assertTrue(cClass.isSubClassOf(aClass));
		  assertTrue(cClass.isSubClassOf(bClass));
		  db.command(new OCommandSQL("alter class sqlC superclass -sqlA")).execute();
		  oSchema.reload();
		  assertFalse(cClass.isSubClassOf(aClass));
		  assertTrue(cClass.isSubClassOf(bClass));
	  }
	  
	  @Test
	  public void testCreationBySql()
	  {
		  final OSchema oSchema = db.getMetadata().getSchema();

		  db.command(new OCommandSQL("create class sql2A abstract")).execute();
		  db.command(new OCommandSQL("create class sql2B abstract")).execute();
		  db.command(new OCommandSQL("create class sql2C extends sql2A, sql2B abstract")).execute();
		  oSchema.reload();
		  OClass aClass = oSchema.getClass("sql2A");
		  OClass bClass = oSchema.getClass("sql2B");
		  OClass cClass = oSchema.getClass("sql2C");
		  assertNotNull(aClass);
		  assertNotNull(bClass);
		  assertNotNull(cClass);
		  assertTrue(cClass.isSubClassOf(aClass));
		  assertTrue(cClass.isSubClassOf(bClass));
	  }
	  
	  @Test(expectedExceptions={OSchemaException.class}, expectedExceptionsMessageRegExp=".*recursion.*")
	  public void testPreventionOfCycles()
	  {
		  final OSchema oSchema = db.getMetadata().getSchema();
		  OClass aClass = oSchema.createAbstractClass("cycleA");
		  OClass bClass = oSchema.createAbstractClass("cycleB", aClass);
		  OClass cClass = oSchema.createAbstractClass("cycleC", bClass);
		  
		  aClass.setSuperClasses(Arrays.asList(cClass));
	  }
}
