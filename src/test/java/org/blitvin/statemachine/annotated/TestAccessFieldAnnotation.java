
package org.blitvin.statemachine.annotated;

import java.lang.annotation.Annotation;

public class TestAccessFieldAnnotation {
	String namevalue;
	public TestAccessFieldAnnotation(){
		Annotation[] an =this.getClass().getAnnotations();
		for(Annotation cur:an){
			System.out.println(cur.toString());
		}
	}
	
	public String toString(){
		return null;
	}
	
	public static void main(String[] args){
		@TestAnnotation(name="aaa", value="bbb")
		TestAccessFieldAnnotation t= new TestAccessFieldAnnotation();
	}
}