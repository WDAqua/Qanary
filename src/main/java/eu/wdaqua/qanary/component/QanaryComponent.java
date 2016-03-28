package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.message.QanaryMessage;

/**
 * represent the behavior of an annotator following the Qanary methodology
 * 
 * @author AnBo
 *
 */
public interface QanaryComponent {

	/**
	 * needs to be implemented for any new Qanary component
	 * 
	 * @param myQanaryMessage
	 * @return
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage);

}
