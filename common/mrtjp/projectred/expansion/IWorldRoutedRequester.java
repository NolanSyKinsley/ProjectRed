package mrtjp.projectred.expansion;

import mrtjp.projectred.core.utils.ItemKeyStack;

public interface IWorldRoutedRequester extends IWorldRouter {

    public void trackedItemLost(ItemKeyStack s);

    public void trackedItemReceived(ItemKeyStack s);
}
