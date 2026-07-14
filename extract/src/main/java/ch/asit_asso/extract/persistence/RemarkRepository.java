package ch.asit_asso.extract.persistence;

import java.util.List;
import ch.asit_asso.extract.domain.Process;
import ch.asit_asso.extract.domain.Remark;
import ch.asit_asso.extract.utils.AlphabeticalOrder;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * A link between the process data objects and the data source.
 *
 * @author Yves Grasset
 */
public interface RemarkRepository extends PagingAndSortingRepository<Remark, Integer> {

    /**
     * Obtains all the predefined remarks in the data source and sorts them based on their title, ignoring
     * the case and the accents.
     *
     * @return a collection of all the predefined remarks in the data source
     */
    default List<Remark> findAllSortedByTitle() {
        return AlphabeticalOrder.sort(this.findAll(), Remark::getTitle);
    }

    /**
     * Obtains a predefined request based on its title.
     *
     * @param title the title of the remark to find
     * @return the predefined remark that matches the given title, or <code>null</code> if none was found
     */
    Remark findByTitle(String title);
}
