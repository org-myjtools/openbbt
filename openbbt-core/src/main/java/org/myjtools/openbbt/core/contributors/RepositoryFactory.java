package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.persistence.Repository;

@ExtensionPoint
public interface RepositoryFactory {

	<T extends Repository> T createRepository(Class<T>type);

}