package lt.emasina.resthub.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(MdColumn.class)
public abstract class MdColumn_ extends lt.emasina.resthub.model.MdEntity_ {

	public static volatile SingularAttribute<MdColumn, Integer> number;
	public static volatile SingularAttribute<MdColumn, String> name;
	public static volatile SingularAttribute<MdColumn, MdType> type;
	public static volatile SingularAttribute<MdColumn, MdTable> table;

}

